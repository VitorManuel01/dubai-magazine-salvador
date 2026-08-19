import { type ChangeEvent, type FormEvent, useState } from 'react';
import axios from 'axios';
import { useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { validarArquivoOds } from '../utils/validacaoArquivos';
import './ImportacaoProdutos.css';

interface ResultadoImportacaoEstoque {
  arquivo: string;
  registrosLidos: number;
  produtosAtualizados: number;
  codigosIgnorados: number;
  importadoEm: string;
  duracaoMilissegundos: number;
}

interface ErroImportacao {
  erro?: string;
  message?: string;
}

type FaseImportacao = 'pronto' | 'enviando' | 'processando' | 'concluido' | 'erro';

function ImportacaoEstoque() {
  const queryClient = useQueryClient();
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [fase, setFase] = useState<FaseImportacao>('pronto');
  const [progresso, setProgresso] = useState(0);
  const [resultado, setResultado] = useState<ResultadoImportacaoEstoque | null>(null);
  const [erro, setErro] = useState('');

  const selecionarArquivo = async (event: ChangeEvent<HTMLInputElement>) => {
    const selecionado = event.target.files?.[0] ?? null;
    setResultado(null);
    setErro('');
    setProgresso(0);
    setFase('pronto');

    if (!selecionado) {
      setArquivo(null);
      return;
    }

    const erroValidacao = await validarArquivoOds(selecionado);
    if (erroValidacao) {
      setArquivo(null);
      setErro(erroValidacao);
      setFase('erro');
      event.target.value = '';
      return;
    }
    setArquivo(selecionado);
  };

  const importar = async (event: FormEvent) => {
    event.preventDefault();
    if (!arquivo) {
      setErro('Selecione o arquivo ODS do inventário do Santri.');
      setFase('erro');
      return;
    }

    const formData = new FormData();
    formData.append('arquivo', arquivo);
    setErro('');
    setResultado(null);
    setProgresso(0);
    setFase('enviando');

    try {
      const response = await axios.post<ResultadoImportacaoEstoque>(
        '/admin/importacoes/estoque',
        formData,
        {
          onUploadProgress: (progressEvent) => {
            if (!progressEvent.total) return;
            const percentual = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total,
            );
            setProgresso(percentual);
            if (percentual >= 100) setFase('processando');
          },
        },
      );

      setResultado(response.data);
      setFase('concluido');
      setProgresso(100);
      await queryClient.invalidateQueries({ queryKey: ['dados-produto'] });
    } catch (error) {
      if (axios.isAxiosError<ErroImportacao>(error)) {
        setErro(
          error.response?.data?.erro
          ?? error.response?.data?.message
          ?? 'Não foi possível importar o inventário.',
        );
      } else {
        setErro('Não foi possível importar o inventário.');
      }
      setFase('erro');
    }
  };

  const ocupada = fase === 'enviando' || fase === 'processando';
  const duracaoSegundos = resultado
    ? (resultado.duracaoMilissegundos / 1000).toLocaleString('pt-BR', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      })
    : '';

  return (
    <div className="import-page">
      <div className="import-heading">
        <div>
          <span className="import-eyebrow">Área administrativa</span>
          <h1>Atualizar estoque pelo inventário</h1>
          <p>
            Envie o relatório de Inventário do Santri. A aplicação lê somente o código
            do produto e a quantidade, atualizando apenas produtos já cadastrados.
          </p>
        </div>
        <div className="import-heading__actions">
          <Link className="btn btn-outline-secondary" to="/minha-conta">
            <i className="bi bi-arrow-left" />
            Voltar para Minha Conta
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-produtos">
            Importar produtos
          </Link>
          <Link className="btn btn-outline-secondary" to="/produtos">
            Voltar ao catálogo
          </Link>
        </div>
      </div>

      <section className="import-card">
        <div className="import-card__icon" aria-hidden="true">
          <i className="bi bi-boxes" />
        </div>

        <form onSubmit={importar}>
          <label className="import-file" htmlFor="arquivo-estoque">
            <span className="import-file__title">
              {arquivo ? arquivo.name : 'Selecionar inventário ODS'}
            </span>
            <span className="import-file__hint">
              {arquivo
                ? `${(arquivo.size / 1024 / 1024).toFixed(2)} MB`
                : 'Tamanho máximo: 20 MB'}
            </span>
            <input
              id="arquivo-estoque"
              type="file"
              accept=".ods"
              onChange={selecionarArquivo}
              disabled={ocupada}
            />
          </label>

          <div className="import-notice">
            <i className="bi bi-shield-check" />
            <p>
              Códigos que não existem no banco serão ignorados. Nome, preço, imagens,
              categorias e visibilidade no site não serão alterados. Estoque zero fará
              um produto que já esteja visível aparecer como ESGOTADO.
            </p>
          </div>

          {ocupada && (
            <div className="import-progress" aria-live="polite">
              <div className="import-progress__labels">
                <span>
                  {fase === 'enviando'
                    ? `Enviando arquivo: ${progresso}%`
                    : 'Arquivo enviado. Atualizando os estoques...'}
                </span>
                <span>{progresso}%</span>
              </div>
              <div className="import-progress__track">
                <span style={{ width: `${progresso}%` }} />
              </div>
            </div>
          )}

          {erro && (
            <div className="import-feedback import-feedback--error" role="alert">
              <i className="bi bi-exclamation-circle" />
              <span>{erro}</span>
            </div>
          )}

          <button
            className="btn btn-primary import-submit"
            type="submit"
            disabled={!arquivo || ocupada}
          >
            {fase === 'enviando' && 'Enviando...'}
            {fase === 'processando' && 'Atualizando...'}
            {!ocupada && 'Atualizar estoque'}
          </button>
        </form>
      </section>

      {resultado && (
        <section className="import-result" aria-live="polite">
          <div className="import-result__heading">
            <i className="bi bi-check-circle-fill" />
            <div>
              <h2>Estoque atualizado</h2>
              <p>
                {resultado.arquivo} processado em {duracaoSegundos} segundos.
              </p>
            </div>
          </div>

          <div className="import-result__grid">
            <div>
              <span>Registros lidos</span>
              <strong>{resultado.registrosLidos.toLocaleString('pt-BR')}</strong>
            </div>
            <div>
              <span>Produtos atualizados</span>
              <strong>{resultado.produtosAtualizados.toLocaleString('pt-BR')}</strong>
            </div>
            <div>
              <span>Códigos ignorados</span>
              <strong>{resultado.codigosIgnorados.toLocaleString('pt-BR')}</strong>
            </div>
          </div>

          {resultado.codigosIgnorados > 0 && (
            <p className="import-result__note">
              Os códigos ignorados pertencem a itens que constam no inventário, mas não
              estão cadastrados no catálogo deste site.
            </p>
          )}
        </section>
      )}
    </div>
  );
}

export default ImportacaoEstoque;

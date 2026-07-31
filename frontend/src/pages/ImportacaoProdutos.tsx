import { ChangeEvent, FormEvent, useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { validarArquivoOds } from '../utils/validacaoArquivos';
import './ImportacaoProdutos.css';

interface ResultadoImportacao {
  arquivo: string;
  categoriasLidas: number;
  categoriasCriadas: number;
  categoriasAtualizadas: number;
  produtosLidos: number;
  produtosCriados: number;
  produtosAtualizados: number;
  linhasIgnoradas: number;
  importadoEm: string;
  duracaoMilissegundos: number;
}

interface ErroImportacao {
  erro?: string;
  message?: string;
}

type FaseImportacao = 'pronto' | 'enviando' | 'processando' | 'concluido' | 'erro';

function ImportacaoProdutos() {
  const queryClient = useQueryClient();
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [fase, setFase] = useState<FaseImportacao>('pronto');
  const [progresso, setProgresso] = useState(0);
  const [resultado, setResultado] = useState<ResultadoImportacao | null>(null);
  const [erro, setErro] = useState('');

  const handleArquivo = async (event: ChangeEvent<HTMLInputElement>) => {
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
      setErro('Selecione o arquivo ODS da relação analítica de produtos.');
      setFase('erro');
      return;
    }
    if (!arquivo.name.toLowerCase().endsWith('.ods')) {
      setErro('O arquivo selecionado precisa ter a extensão .ods.');
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
      const response = await axios.post<ResultadoImportacao>(
        '/admin/importacoes/produtos',
        formData,
        {
          onUploadProgress: (progressEvent) => {
            if (!progressEvent.total) return;
            const percentual = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setProgresso(percentual);
            if (percentual >= 100) {
              setFase('processando');
            }
          },
        }
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
          ?? 'Não foi possível importar a relação de produtos.'
        );
      } else {
        setErro('Não foi possível importar a relação de produtos.');
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
          <h1>Importar relação de produtos</h1>
          <p>
            Envie a Relação de Produtos por Grupo gerada no Santri com produtos ativos e
            estoque físico positivo.
          </p>
        </div>
        <div className="import-heading__actions">
          <Link className="btn btn-outline-primary" to="/admin/vitrines-home">
            Gerenciar vitrines
          </Link>
          <Link className="btn btn-outline-secondary" to="/produtos">
            Voltar ao catálogo
          </Link>
        </div>
      </div>

      <section className="import-card">
        <div className="import-card__icon" aria-hidden="true">
          <i className="bi bi-file-earmark-spreadsheet" />
        </div>

        <form onSubmit={importar}>
          <label className="import-file" htmlFor="arquivo-inventario">
            <span className="import-file__title">
              {arquivo ? arquivo.name : 'Selecionar arquivo ODS'}
            </span>
            <span className="import-file__hint">
              {arquivo
                ? `${(arquivo.size / 1024 / 1024).toFixed(2)} MB`
                : 'Tamanho máximo: 20 MB'}
            </span>
            <input
              id="arquivo-inventario"
              type="file"
              accept=".ods"
              onChange={handleArquivo}
              disabled={ocupada}
            />
          </label>

          <div className="import-notice">
            <i className="bi bi-shield-check" />
            <p>
              Produtos existentes serão atualizados pelo código Santri. Nome público e imagens
              serão preservados. Produtos ausentes não serão excluídos, mas ficarão indisponíveis
              e ocultos até reaparecerem em uma importação.
            </p>
          </div>

          {ocupada && (
            <div className="import-progress" aria-live="polite">
              <div className="import-progress__labels">
                <span>
                  {fase === 'enviando'
                    ? `Enviando arquivo: ${progresso}%`
                    : 'Arquivo enviado. Processando a relação de produtos...'}
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
            {fase === 'processando' && 'Importando...'}
            {!ocupada && 'Importar relação'}
          </button>
        </form>
      </section>

      {resultado && (
        <section className="import-result" aria-live="polite">
          <div className="import-result__heading">
            <i className="bi bi-check-circle-fill" />
            <div>
              <h2>Importação concluída</h2>
              <p>
                {resultado.arquivo} processado em {duracaoSegundos} segundos.
              </p>
            </div>
          </div>

          <div className="import-result__grid">
            <div>
              <span>Produtos lidos</span>
              <strong>{resultado.produtosLidos.toLocaleString('pt-BR')}</strong>
            </div>
            <div>
              <span>Produtos novos</span>
              <strong>{resultado.produtosCriados.toLocaleString('pt-BR')}</strong>
            </div>
            <div>
              <span>Produtos atualizados</span>
              <strong>{resultado.produtosAtualizados.toLocaleString('pt-BR')}</strong>
            </div>
            <div>
              <span>Categorias lidas</span>
              <strong>{resultado.categoriasLidas.toLocaleString('pt-BR')}</strong>
            </div>
          </div>

          {resultado.linhasIgnoradas > 0 && (
            <p className="import-result__note">
              {resultado.linhasIgnoradas.toLocaleString('pt-BR')} linhas sem produto ativo
              e estoque positivo foram ignoradas.
            </p>
          )}
        </section>
      )}
    </div>
  );
}

export default ImportacaoProdutos;

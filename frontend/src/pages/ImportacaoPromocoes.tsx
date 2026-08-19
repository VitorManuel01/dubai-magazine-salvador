import { ChangeEvent, FormEvent, useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { validarArquivoOds } from '../utils/validacaoArquivos';
import './ImportacaoProdutos.css';

interface ResultadoImportacao {
  arquivo: string;
  registrosLidos: number;
  produtosAtualizados: number;
  promocoesAtivas: number;
  promocoesDesativadas: number;
  codigosNaoEncontrados: number;
  linhasIgnoradas: number;
  importadoEm: string;
  duracaoMs: number;
}

type Fase = 'pronto' | 'enviando' | 'processando' | 'concluido' | 'erro';

function ImportacaoPromocoes() {
  const queryClient = useQueryClient();
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [fase, setFase] = useState<Fase>('pronto');
  const [progresso, setProgresso] = useState(0);
  const [resultado, setResultado] = useState<ResultadoImportacao | null>(null);
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
    if (!arquivo) return;
    const dados = new FormData();
    dados.append('arquivo', arquivo);
    setFase('enviando');
    setErro('');
    setResultado(null);
    try {
      const response = await axios.post<ResultadoImportacao>(
        '/admin/importacoes/promocoes',
        dados,
        {
          onUploadProgress: (evento) => {
            if (!evento.total) return;
            const percentual = Math.round((evento.loaded * 100) / evento.total);
            setProgresso(percentual);
            if (percentual >= 100) setFase('processando');
          },
        },
      );
      setResultado(response.data);
      setFase('concluido');
      setProgresso(100);
      await queryClient.invalidateQueries({ queryKey: ['dados-produto'] });
    } catch (falha) {
      const mensagem = axios.isAxiosError<{ erro?: string; message?: string }>(falha)
        ? falha.response?.data?.erro ?? falha.response?.data?.message
        : null;
      setErro(mensagem ?? 'Não foi possível importar as promoções.');
      setFase('erro');
    }
  };

  const ocupada = fase === 'enviando' || fase === 'processando';

  return (
    <div className="import-page">
      <div className="import-heading">
        <div>
          <span className="import-eyebrow">Área administrativa</span>
          <h1>Importar promoções de venda</h1>
          <p>Envie o relatório Promoções de Venda gerado no Santri.</p>
        </div>
        <div className="import-heading__actions">
          <Link className="btn btn-outline-secondary" to="/minha-conta">
            <i className="bi bi-arrow-left" />
            Voltar para Minha Conta
          </Link>
          <Link className="btn btn-outline-primary" to="/admin/importacao-produtos">Importar produtos</Link>
          <Link className="btn btn-outline-secondary" to="/produtos">Voltar ao catálogo</Link>
        </div>
      </div>

      <section className="import-card">
        <div className="import-card__icon" aria-hidden="true"><i className="bi bi-tags" /></div>
        <form onSubmit={importar}>
          <label className="import-file" htmlFor="arquivo-promocoes">
            <span className="import-file__title">{arquivo ? arquivo.name : 'Selecionar arquivo ODS'}</span>
            <span className="import-file__hint">
              {arquivo ? `${(arquivo.size / 1024 / 1024).toFixed(2)} MB` : 'Tamanho máximo: 20 MB'}
            </span>
            <input
              id="arquivo-promocoes"
              type="file"
              accept=".ods,application/vnd.oasis.opendocument.spreadsheet"
              onChange={selecionarArquivo}
              disabled={ocupada}
            />
          </label>
          <div className="import-notice">
            <i className="bi bi-shield-check" />
            <p>
              Somente produtos já cadastrados serão atualizados. Promoções antigas ausentes no
              novo relatório serão removidas e códigos desconhecidos serão ignorados.
            </p>
          </div>
          {ocupada && (
            <div className="import-progress" aria-live="polite">
              <div className="import-progress__labels">
                <span>{fase === 'enviando' ? `Enviando: ${progresso}%` : 'Processando promoções...'}</span>
                <span>{progresso}%</span>
              </div>
              <div className="import-progress__track"><span style={{ width: `${progresso}%` }} /></div>
            </div>
          )}
          {erro && <div className="import-feedback import-feedback--error" role="alert">{erro}</div>}
          <button className="btn btn-primary import-submit" type="submit" disabled={!arquivo || ocupada}>
            {ocupada ? 'Importando...' : 'Importar promoções'}
          </button>
        </form>
      </section>

      {resultado && (
        <section className="import-result" aria-live="polite">
          <div className="import-result__heading">
            <i className="bi bi-check-circle-fill" />
            <div><h2>Importação concluída</h2><p>{resultado.arquivo}</p></div>
          </div>
          <div className="import-result__grid">
            <div><span>Registros lidos</span><strong>{resultado.registrosLidos}</strong></div>
            <div><span>Produtos atualizados</span><strong>{resultado.produtosAtualizados}</strong></div>
            <div><span>Promoções ativas</span><strong>{resultado.promocoesAtivas}</strong></div>
            <div><span>Códigos não encontrados</span><strong>{resultado.codigosNaoEncontrados}</strong></div>
          </div>
          {resultado.promocoesDesativadas > 0 && (
            <p className="import-result__note">{resultado.promocoesDesativadas} promoções vieram desativadas.</p>
          )}
        </section>
      )}
    </div>
  );
}

export default ImportacaoPromocoes;

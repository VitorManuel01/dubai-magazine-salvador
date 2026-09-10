import Decimal from 'decimal.js';
import { PrecosPersonalizados } from '../../interface/PrecosPersonalizados';
import './precosPersonalizados.css';

const moeda = (valor: number) => valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export function PrecoPersonalizado({ produto }: { produto: PrecosPersonalizados }) {
  if (!produto.usarPrecosPersonalizados || produto.precoAVista == null
    || produto.precoCartaoParc == null || !produto.maxParcelamento) return null;
  const parcela = new Decimal(produto.precoCartaoParc)
    .div(produto.maxParcelamento).toDecimalPlaces(2, Decimal.ROUND_HALF_UP).toNumber();
  return (
    <div className="preco-personalizado">
      <strong className="preco-personalizado__vista">{moeda(produto.precoAVista)} <small>à vista</small></strong>
      <span className="preco-personalizado__cartao">{moeda(produto.precoCartaoParc)} no cartão</span>
      <small className="preco-personalizado__parcelas">
        {produto.maxParcelamento === 1 ? 'Em 1x' : `Em até ${produto.maxParcelamento}x de aproximadamente ${moeda(parcela)}`}
      </small>
    </div>
  );
}

import { FormPrecos } from '../../utils/precosPersonalizados';
import './precosPersonalizados.css';

export function EditorPrecosProduto({ value, onChange, disabled }: {
  value: FormPrecos; onChange: (value: FormPrecos) => void; disabled: boolean;
}) {
  return (
    <div className="produto-precos-editor">
      <label className="produto-precos-switch">
        <input type="checkbox" role="switch" checked={value.usarPrecosPersonalizados}
          disabled={disabled} onChange={(e) => onChange({ ...value, usarPrecosPersonalizados: e.target.checked })} />
        <span>Mostrar valores personalizados</span>
      </label>
      <small>{value.usarPrecosPersonalizados
        ? 'Os valores abaixo substituirão o preço e a promoção do Santri nas telas do site.'
        : 'Mostrando o preço do Santri. Os valores manuais continuam guardados.'}</small>
      {value.usarPrecosPersonalizados && <>
        <label>Preço à vista (R$)
          <input type="number" inputMode="decimal" min="0.01" max="9999999999.99" step="0.01"
            value={value.precoAVista} disabled={disabled}
            onChange={(e) => onChange({ ...value, precoAVista: e.target.value })} />
        </label>
        <label>Preço total no cartão (R$)
          <input type="number" inputMode="decimal" min="0.01" max="9999999999.99" step="0.01"
            value={value.precoCartaoParc} disabled={disabled}
            onChange={(e) => onChange({ ...value, precoCartaoParc: e.target.value })} />
        </label>
        <label>Máximo de parcelas
          <input type="number" inputMode="numeric" min="1" max="36" step="1"
            value={value.maxParcelamento} disabled={disabled}
            onChange={(e) => onChange({ ...value, maxParcelamento: e.target.value })} />
        </label>
        <small>Valores finais, sem acrescentar o IPI do Santri. O total no cartão será dividido pelo número de parcelas.</small>
      </>}
    </div>
  );
}

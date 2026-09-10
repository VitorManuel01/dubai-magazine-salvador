import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formularioPrecos, validarPrecos } from '../src/utils/precosPersonalizados.ts';

const form = { usarPrecosPersonalizados: true, precoAVista: '1000', precoCartaoParc: '1200.99', maxParcelamento: '10' };
test('Preços positivos com centavos e parcelas inteiras são aceitos', () => {
  assert.equal(validarPrecos(form), null);
});
test('Ativação exige os três valores válidos', () => {
  for (const campo of ['precoAVista', 'precoCartaoParc', 'maxParcelamento']) {
    for (const valor of ['', '0', '-1', 'NaN', 'Infinity', '1.001']) {
      assert.ok(validarPrecos({ ...form, [campo]: valor }));
    }
  }
  assert.ok(validarPrecos({ ...form, maxParcelamento: '37' }));
  assert.ok(validarPrecos({ ...form, precoAVista: '10000000000' }));
});
test('Desligado permite campos vazios e recarrega os valores salvos', () => {
  assert.equal(validarPrecos(formularioPrecos({ usarPrecosPersonalizados: false, precoAVista: null, precoCartaoParc: null, maxParcelamento: null })), null);
  assert.equal(formularioPrecos({ usarPrecosPersonalizados: false, precoAVista: 100, precoCartaoParc: 120, maxParcelamento: 3 }).precoCartaoParc, '120');
});

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { loadConfigFromFile } from 'vite';

const resultado = await loadConfigFromFile({ command: 'serve', mode: 'preproduction' });
const options = resultado.config.preview.proxy['/api'];

function encaminhar(remoteAddress, headers) {
  const proxy = new EventEmitter();
  options.configure(proxy);
  const enviados = new Map(Object.entries(headers).map(([key, value]) => [key.toLowerCase(), value]));
  proxy.emit('proxyReq', {
    removeHeader: nome => enviados.delete(nome.toLowerCase()),
    setHeader: (nome, valor) => enviados.set(nome.toLowerCase(), valor),
  }, { socket: { remoteAddress }, headers });
  return enviados;
}

test('preview mantém origem loopback e não concatena X-Forwarded-For do cliente', () => {
  assert.equal(resultado.config.preview.host, '127.0.0.1');
  assert.equal(options.xfwd, false);
});

test('proxy do tunnel elimina cabeçalhos forjados e conserva IP informado pela Cloudflare', () => {
  const enviados = encaminhar('127.0.0.1', {
    forwarded: 'for=10.0.0.1;proto=http;host=evil.test',
    'x-forwarded-for': '10.0.0.1',
    'x-forwarded-host': 'evil.test',
    'x-forwarded-prefix': '/admin',
    'cf-connecting-ip': '192.0.2.80',
  });
  assert.equal(enviados.has('forwarded'), false);
  assert.equal(enviados.has('x-forwarded-host'), false);
  assert.equal(enviados.has('x-forwarded-prefix'), false);
  assert.equal(enviados.get('x-forwarded-for'), '192.0.2.80');
  assert.equal(enviados.get('x-forwarded-proto'), 'https');
});

test('IP da Cloudflare não é aceito de conexão externa nem com valor inválido', () => {
  assert.equal(encaminhar('192.0.2.10', { 'cf-connecting-ip': '192.0.2.80' })
    .get('x-forwarded-for'), '192.0.2.10');
  assert.equal(encaminhar('127.0.0.1', { 'cf-connecting-ip': '192.0.2.80, 10.0.0.1' })
    .get('x-forwarded-for'), '127.0.0.1');
});

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { pertenceApi } from '../src/utils/destinoApi.ts';

const api = new URL('https://catalogo.example/api');

test('Bearer pode acompanhar endpoints do mesmo prefixo e origem', () => {
  assert.equal(pertenceApi(new URL('/api/produto?busca=abc', api), api), true);
});

test('Bearer não acompanha outra origem, porta ou protocolo', () => {
  for (const destino of [
    'https://externo.example/api/produto',
    'https://catalogo.example:8443/api/produto',
    'http://catalogo.example/api/produto',
    'https://catalogo.example.evil.test/api/produto',
  ]) assert.equal(pertenceApi(new URL(destino), api), false);
});

test('Bearer não acompanha caminhos fora da API nem prefixos parecidos', () => {
  for (const destino of ['/api-externa', '/apifake/produto', '/api/../arquivo', '/assets/logo.svg']) {
    assert.equal(pertenceApi(new URL(destino, api), api), false);
  }
});

test('API local sem prefixo aceita apenas sua própria origem', () => {
  const local = new URL('http://localhost:8081');
  assert.equal(pertenceApi(new URL('/produto', local), local), true);
  assert.equal(pertenceApi(new URL('http://localhost:5173/produto'), local), false);
});

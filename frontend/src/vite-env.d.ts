/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV: 'development' | 'preproduction' | 'production';
  readonly VITE_API_BASE_URL: string;
  readonly VITE_REQUIRE_HTTPS: 'true' | 'false';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

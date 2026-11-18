/// <reference types="vitest" />
import { defineConfig } from "vitest/config";
import { transformWithOxc, type Plugin } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";

// Configuration de la chaîne de construction, reprise de celle du socle
// communautaire (frontend/vite.config.ts) avec trois différences :
// - pas de Module Federation : ce frontend n'héberge pas de modules distants ;
// - les sources gardent l'extension .js même quand elles contiennent du JSX
//   (héritage de Create React App), que la transformation oxc de Vite refuse
//   selon l'extension : le greffon jsxInJs ci-dessous compile ce JSX avant la
//   chaîne standard, qui garde la main sur le Fast Refresh ;
// - le port HMR vu du navigateur se règle par VITE_HMR_CLIENT_PORT quand le
//   serveur de développement est servi derrière le mandataire nginx.
const hmrClientPort = process.env.VITE_HMR_CLIENT_PORT
  ? Number(process.env.VITE_HMR_CLIENT_PORT)
  : undefined;

function jsxInJs(): Plugin {
  let development = false;
  return {
    name: "civ:jsx-in-js",
    enforce: "pre",
    configResolved(config) {
      development = config.command === "serve" && !config.isProduction;
    },
    async transform(code, id) {
      const [file] = id.split("?");
      if (!file.endsWith(".js") || file.includes("/node_modules/")) {
        return null;
      }
      if (!code.includes("<")) {
        return null;
      }
      const result = await transformWithOxc(code, file, {
        lang: "jsx",
        jsx: { runtime: "automatic", development },
      });
      return { code: result.code, map: result.map };
    },
  };
}

export default defineConfig({
  plugins: [jsxInJs(), react(), svgr()],
  oxc: {
    include: /\.[jt]sx?$/,
    exclude: /node_modules/,
  },
  // Le pré-scan des dépendances du serveur de développement lit lui aussi
  // les sources .js : en JSX, pour la même raison.
  optimizeDeps: {
    rolldownOptions: {
      moduleTypes: { ".js": "jsx" },
    },
  },
  resolve: {
    extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json"],
    alias: [
      { find: /^~/, replacement: "" },
      // react-data-table-component@7.5.3 publie en `main` un paquet CJS sans
      // `exports.__esModule` : l'interopérabilité CJS/ESM de Vite rendrait
      // alors l'objet des exports au lieu du composant. On pointe sur le
      // paquet ESM (son champ `module`).
      {
        find: /^react-data-table-component$/,
        replacement: "react-data-table-component/dist/index.es.js",
      },
    ],
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/setupTests.js",
    server: {
      deps: {
        inline: ["@carbon/react", "flatpickr"],
      },
    },
    exclude: ["**/node_modules/**", "**/dist/**", "**/cypress/**"],
  },
  server: {
    port: 80,
    host: true,
    https: !!process.env.HTTPS,
    open: false,
    hmr: hmrClientPort ? { clientPort: hmrClientPort } : undefined,
  },
});

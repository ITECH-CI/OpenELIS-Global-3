// Configuration ESLint à plat (ESLint 9+), qui remplace .eslintrc.js.
//
// Reprise de frontend/eslint.config.js du socle communautaire, sans ce qui
// dépend d'outils absents ici : le harnais Playwright et les règles locales
// d'eslint-local-rules/. Extension .mjs : le paquet n'est pas déclaré
// `"type": "module"` (cypress.config.js reste en CommonJS).
//
// Ordre : ignorés → JS/JSX → TS/TSX → tests.

import js from "@eslint/js";
import globals from "globals";
import reactPlugin from "eslint-plugin-react";
import reactHooksPlugin from "eslint-plugin-react-hooks";
import prettierPlugin from "eslint-plugin-prettier";
import prettierConfig from "eslint-config-prettier";
import jestPlugin from "eslint-plugin-jest";
import tseslint from "typescript-eslint";

export default [
  // ─── Global ignores ─────────────────────────────────────────────────
  // Migrated from the former .eslintignore. Flat config uses the
  // top-level `ignores` key instead of a separate file.
  {
    ignores: [
      "node_modules/",
      "dist/",
      "build/",
      "coverage/",
      "cypress/videos/",
      "cypress/screenshots/",
      ".vite/",
      "public/",
      "scripts/",
      "**/*.min.js",
    ],
  },

  // ─── Base JS/JSX ────────────────────────────────────────────────────
  js.configs.recommended,
  {
    files: ["**/*.{js,jsx,mjs,cjs}"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    plugins: {
      react: reactPlugin,
      "react-hooks": reactHooksPlugin,
      prettier: prettierPlugin,
    },
    settings: {
      react: { version: "detect" },
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
      ...prettierConfig.rules,
      "react/prop-types": "off",
      "react/react-in-jsx-scope": "off",
      "react/no-unescaped-entities": "warn",
      "react-hooks/exhaustive-deps": "off",
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      "prettier/prettier": "warn",
    },
  },

  // ─── TypeScript ─────────────────────────────────────────────────────
  ...tseslint.configs.recommended.map((cfg) => ({
    ...cfg,
    files: ["**/*.{ts,tsx}"],
  })),
  {
    files: ["**/*.{ts,tsx}"],
    plugins: {
      react: reactPlugin,
      "react-hooks": reactHooksPlugin,
    },
    settings: {
      react: { version: "detect" },
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      "react/prop-types": "warn",
      "react/react-in-jsx-scope": "off",
      "react/display-name": "off",
      "no-case-declarations": "off",
      "@typescript-eslint/no-empty-function": "warn",
      "@typescript-eslint/no-empty-interface": "warn",
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      // Disable the base rule; the TS-aware version above supersedes it.
      "no-unused-vars": "off",
    },
  },

  // ─── Jest unit tests ────────────────────────────────────────────────
  {
    files: ["**/*.test.{js,jsx,ts,tsx}"],
    plugins: { jest: jestPlugin },
    languageOptions: {
      globals: { ...globals.jest },
    },
    rules: {
      "jest/expect-expect": "error",
      "jest/no-disabled-tests": "warn",
      "jest/no-focused-tests": "error",
      "jest/valid-expect": "error",
    },
  },
];

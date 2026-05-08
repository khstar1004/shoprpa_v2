# ShopRPA Frontend

<div align="center">

**🎨 Modern Frontend Platform for RPA Applications**

[![Node.js](https://img.shields.io/badge/node.js-22+-green.svg)](https://nodejs.org/)
[![Vue](https://img.shields.io/badge/vue-3+-4FC08D.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/typescript-5.9+-blue.svg)](https://www.typescriptlang.org/)
[![pnpm](https://img.shields.io/badge/pnpm-9+-orange.svg)](https://pnpm.io/)
[![Electron](https://img.shields.io/badge/electron-22+-purple.svg)](https://www.electronjs.org/)

</div>

## 📑 Table of Contents

- [ShopRPA Frontend](#shoprpa-frontend)
  - [📑 Table of Contents](#-table-of-contents)
  - [📋 Overview](#-overview)
  - [✨ Key Features](#-key-features)
  - [🛠️ Tech Stack](#️-tech-stack)
  - [🚀 Quick Start](#-quick-start)
    - [System Requirements](#system-requirements)
    - [Development Setup](#development-setup)
    - [Build \& Deploy](#build--deploy)
  - [📦 Package Structure](#-package-structure)
    - [Core Packages](#core-packages)
    - [Development Tools](#development-tools)
  - [🏗️ Architecture Overview](#️-architecture-overview)
    - [Technology Stack Details](#technology-stack-details)

## 📋 Overview

ShopRPA Frontend is a modern frontend platform built for RPA applications. It provides a comprehensive solution for building both web-based and desktop RPA applications with a unified codebase.

The platform features a monorepo architecture using pnpm workspaces, supporting multiple application types including web applications, desktop applications, and browser plugins, all sharing common components and utilities.

## ✨ Key Features

- 🚀 **High Performance** - Vite-powered build system with optimized bundling and lazy loading
- 🔒 **Type Safety** - Full TypeScript support with strict type checking
- 🔧 **Easy Integration** - Modular package structure with workspace dependencies
- 📊 **Real-time Development** - Hot module replacement and fast refresh
- 🌍 **Multi-Platform Support** - Web, desktop, and browser extension support
- 📈 **Scalable Architecture** - Monorepo with shared components and utilities

## 🛠️ Tech Stack

**Frontend Framework**: Vue 3 + TypeScript + Vite
**UI Components**: Ant Design Vue + VXE Table
**Desktop App**: Electron
**State Management**: Pinia
**Package Manager**: pnpm workspaces
**Testing**: Vitest + Vue Test Utils
**Code Quality**: ESLint + Prettier
**Build Tools**: Vite + Rollup
**Styling**: Sass + Tailwind CSS
**Internationalization**: i18next + vue-i18n

## 🚀 Quick Start

### System Requirements

- **Node.js**: >= 22
- **pnpm**: >= 9
- **Operating System**: Windows 10/11, macOS, or Linux

### Development Setup

```bash
# Clone the repository
git clone <your-shoprpa-repository-url>
cd astron-rpa/frontend

# Install dependencies
pnpm install

# Configure environment variables
pnpm set-env

# Start web development server
pnpm dev:web

# Start desktop app (development mode)
pnpm dev:desktop
```

### Build & Deploy

```bash
# Build web application
pnpm build:web

# Build desktop application
pnpm verify:release
pnpm build:desktop

# Check whether this host can create the Windows installer
pnpm doctor:installer

# Build Windows NSIS installer
pnpm build:installer

# Run tests
pnpm test

# Run tests with UI
pnpm test:ui

# Lint and fix code
pnpm lint:fix

# Generate internationalization files
pnpm i18n
```

## 📦 Package Structure

### Core Packages

- **@rpa/web-app**: Main web application
- **@rpa/electron-app**: Desktop application
- **@rpa/browser-plugin**: Browser extension
- **@rpa/components**: Shared UI components
- **@rpa/shared**: Shared tools
- **@rpa/locales**: Internationalization resources

### Development Tools

- **ESLint Configuration**: Antfu's ESLint config
- **Testing**: Vitest with UI support
- **Internationalization**: LobeHub i18n CLI
- **Build Tools**: Vite with multiple build modes

## 🏗️ Architecture Overview

```
Frontend Monorepo
├── packages/
│   ├── web-app/           # Vue 3 Web Application
│   ├── electron-app/      # Electron Desktop App
│   ├── browser-plugin/    # Browser Extension
│   ├── components/        # Shared Components
│   ├── types/            # Type Definitions
│   ├── tokens/           # Design Tokens
│   └── locales/          # i18n Resources
├── node_modules/         # Dependencies
├── package.json          # Root Package Config
├── pnpm-workspace.yaml   # Workspace Configuration
└── vitest.config.mts     # Test Configuration
```

### Technology Stack Details

**Web Application**

- Vue 3 with Composition API
- TypeScript for type safety
- Vite for build tooling
- Ant Design Vue for UI components
- Pinia for state management
- Vue Router for navigation

**Desktop Application**

- Electron for native desktop capabilities
- Node backend with Web frontend
- Native system integration
- Cross-platform compatibility

**Shared Infrastructure**

- pnpm workspaces for monorepo management
- Shared component library
- Common type definitions
- Unified build and test processes

# ShopRPA Desktop

Electron desktop shell for the ShopRPA automation client.

## Development

```bash
pnpm dev
```

## Portable Build

```bash
pnpm build:portable
pnpm verify:portable
```

The portable output is written to `dist/win-portable`. Users can start it with `ShopRPA.cmd`.

## Windows Installer

```bash
pnpm doctor:installer
pnpm build:win
```

The installer build uses `electron-builder` and requires a Windows host where `node.exe` can launch `app-builder.exe` through child processes. If this preflight fails with `EPERM`, use the portable build or run the installer build outside the restricted host policy.

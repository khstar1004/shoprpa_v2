const esbuildShimUrl = new URL('./esbuild-shim.mjs', import.meta.url).href

export async function resolve(specifier, context, nextResolve) {
  if (specifier === 'esbuild') {
    return {
      shortCircuit: true,
      url: esbuildShimUrl,
    }
  }

  return nextResolve(specifier, context)
}

const path = require("path");
const fs = require("fs");

config.files = config.files || [];
config.preprocessors = config.preprocessors || {};

// Tell Karma to serve .wasm and .mjs files
config.files.push({
    pattern: '**/*.wasm',
    included: false,
    served: true,
    watched: false
});

config.files.push({
    pattern: '**/*.mjs',
    included: false,
    served: true,
    watched: false
});

config.mime = config.mime || {};
config.mime['text/javascript'] = ['mjs', 'js'];
config.mime['application/wasm'] = ['wasm'];

config.proxies = config.proxies || {};
config.proxies['/skiko.wasm'] = '/base/kotlin/skiko.wasm';
config.proxies['/skiko.mjs'] = '/base/kotlin/skiko.mjs';

// Timeouts for WebAssembly compilation in headless browser
config.client = config.client || {};
config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 60000;
config.browserNoActivityTimeout = 120000;
config.browserDisconnectTimeout = 60000;

config.customLaunchers = config.customLaunchers || {};
config.customLaunchers.ChromeHeadlessCustom = {
    base: 'ChromeHeadless',
    flags: [
        '--no-sandbox',
        '--disable-gpu',
        '--disable-dev-shm-usage',
        '--enable-features=SharedArrayBuffer'
    ]
};
config.browsers = ['ChromeHeadlessCustom'];

// Create and inject bootstrap script to await Skiko WASM initialization
const setupFile = path.resolve(config.basePath, 'skiko-karma-setup.mjs');
const setupCode = `
import { api } from './kotlin/js-reexport-symbols.mjs';

console.log('[SkikoSetup] Hooking window.__karma__.loaded');
const originalLoaded = window.__karma__.loaded;
window.__karma__.loaded = async function () {
    console.log('[SkikoSetup] Intercepted loaded(). Awaiting Skiko WASM initialization...');
    try {
        await api.awaitSkiko;
        console.log('[SkikoSetup] Skiko WASM initialized successfully!');
        if (originalLoaded) {
            originalLoaded.apply(this, arguments);
        }
    } catch (err) {
        console.error('[SkikoSetup] Failed to initialize Skiko WASM:', err);
        if (window.__karma__) {
            window.__karma__.error('Failed to initialize Skiko WASM: ' + (err && err.stack ? err.stack : err));
        }
    }
};
`;

fs.writeFileSync(setupFile, setupCode);
config.files.unshift(setupFile);
config.preprocessors[setupFile] = ['webpack'];


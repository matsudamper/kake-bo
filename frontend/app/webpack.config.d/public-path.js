// バンドルが追加で読むファイル（.wasm など）を root 絶対で引く。
// 既定の "auto" は読み込んだ <script> の URL から推測するため、深いパスで開くとずれて .wasm が 404 になる。
config.output = config.output || {};
config.output.publicPath = "/";

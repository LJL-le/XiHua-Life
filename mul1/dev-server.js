const http = require('http');
const fs = require('fs');
const path = require('path');
const root = __dirname;
const backend = { host: process.env.BACKEND_HOST || '127.0.0.1', port: Number(process.env.BACKEND_PORT || 8081) };
const types = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'application/javascript; charset=utf-8','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.webp':'image/webp','.woff':'font/woff','.ttf':'font/ttf'};
http.createServer((req,res)=>{
  if(req.url.startsWith('/api/')){
    const opts={hostname:backend.host,port:backend.port,path:req.url.replace(/^\/api/,''),method:req.method,headers:{...req.headers,host:backend.host+':'+backend.port}};
    const proxy=http.request(opts,r=>{res.writeHead(r.statusCode||502,r.headers);r.pipe(res)});
    proxy.on('error',()=>{res.writeHead(502,{'content-type':'text/plain; charset=utf-8'});res.end('Backend 127.0.0.1:8081 is not reachable.');});
    req.pipe(proxy); return;
  }
  const clean=decodeURIComponent(req.url.split('?')[0]);
  const relative = clean === '/' ? 'index.html' : (clean.endsWith('/') ? clean + 'index.html' : clean);
  let file=path.join(root,relative);
  if(!file.startsWith(root)){res.writeHead(403);res.end('Forbidden');return;}
  fs.stat(file,(err,st)=>{
    if(err||!st.isFile()){res.writeHead(404,{'content-type':'text/plain; charset=utf-8'});res.end('Not found');return;}
    res.writeHead(200,{'content-type':types[path.extname(file).toLowerCase()]||'application/octet-stream'});fs.createReadStream(file).pipe(res);
  });
}).listen(5173,()=>console.log('xhul-ife static server: http://localhost:5173'));

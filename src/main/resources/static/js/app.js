import { api, entrar, esc, rotulo, sessao } from './api.js';
import { aoEnviar, campo, toast } from './ui.js';
import { cpfValido } from './ui.js';
import { soDigitos } from './api.js';
import * as dashboard from './telas/dashboard.js';
import * as veiculos from './telas/veiculos.js';
import * as clientes from './telas/clientes.js';
import * as reservas from './telas/reservas.js';
import * as locacoes from './telas/locacoes.js';
import * as pagamentos from './telas/pagamentos.js';
import * as usuarios from './telas/usuarios.js';
import * as portal from './telas/portal.js';

const logo = () => {
  const id = "g" + Math.random().toString(36).slice(2, 8);
  return `<svg viewBox="0 0 32 32" aria-hidden="true"><defs><linearGradient id="${id}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#8b5cf6"/><stop offset="1" stop-color="#3b5bff"/></linearGradient></defs>
<rect width="32" height="32" rx="9" fill="url(#${id})"/><path d="M7 19l2-5a2 2 0 0 1 2-1.3h10a2 2 0 0 1 2 1.3l2 5v4h-2v-1.5H9V23H7z" fill="#fff"/>
<circle cx="11" cy="19" r="1.4" fill="#4c1d95"/><circle cx="21" cy="19" r="1.4" fill="#4c1d95"/></svg><span>Auto<em>Rent</em></span>`;
};

const MENU_STAFF = [
  { rota: 'dashboard', titulo: 'Dashboard', modulo: dashboard },
  { rota: 'veiculos', titulo: 'Veículos', modulo: veiculos },
  { rota: 'clientes', titulo: 'Clientes', modulo: clientes },
  { rota: 'reservas', titulo: 'Reservas', modulo: reservas },
  { rota: 'locacoes', titulo: 'Locações', modulo: locacoes },
  { rota: 'pagamentos', titulo: 'Pagamentos', modulo: pagamentos },
  { rota: 'usuarios', titulo: 'Usuários', modulo: usuarios, so: 'ADMIN' },
];

const MENU_PORTAL = [
  { rota: 'portal', titulo: 'Veículos' },
  { rota: 'portal/reservas', titulo: 'Minhas reservas' },
  { rota: 'portal/locacoes', titulo: 'Minhas locações' },
  { rota: 'portal/pagamentos', titulo: 'Pagamentos' },
  { rota: 'portal/perfil', titulo: 'Meus dados' },
];

const telaLogin = document.getElementById('tela-login');
const telaApp = document.getElementById('tela-app');
let usuario = null;

document.getElementById("logo-lado").innerHTML = logo();

function mostrarLogin() {
  usuario = null;
  telaApp.hidden = true;
  telaLogin.hidden = false;
  document.getElementById('entrar-bloco').hidden = false;
  document.getElementById('cadastro-bloco').hidden = true;
}

const erroLogin = document.getElementById('erro-login');
document.getElementById('form-login').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const form = ev.target;
  const email = form.email.value.trim();
  const senha = form.senha.value;
  erroLogin.hidden = true;
  if (!email || !senha) {
    erroLogin.textContent = 'Informe o e-mail e a senha.';
    erroLogin.hidden = false;
    return;
  }
  const botao = form.querySelector('button');
  botao.disabled = true;
  try {
    usuario = await entrar(email, senha);
    form.reset();
    iniciar();
  } catch (e) {
    erroLogin.textContent = e.status === 401 ? 'E-mail ou senha inválidos.' : e.message;
    erroLogin.hidden = false;
  } finally {
    botao.disabled = false;
  }
});

document.getElementById('ir-cadastro').addEventListener('click', (e) => {
  e.preventDefault();
  document.getElementById('entrar-bloco').hidden = true;
  document.getElementById('cadastro-bloco').hidden = false;
});
document.getElementById('ir-login').addEventListener('click', (e) => {
  e.preventDefault();
  document.getElementById('entrar-bloco').hidden = false;
  document.getElementById('cadastro-bloco').hidden = true;
});

const formCadastro = document.getElementById('form-cadastro');
formCadastro.innerHTML = `
  ${campo('Nome completo', 'nomeCompleto', { classe: 'inteiro' })}
  ${campo('CPF', 'cpf', { mascara: 'cpf', extra: 'inputmode="numeric" placeholder="000.000.000-00"' })}
  ${campo('Telefone', 'telefone', { mascara: 'telefone', extra: 'placeholder="(11) 90000-0000"' })}
  ${campo('E-mail', 'email', { tipo: 'email', classe: 'inteiro' })}
  ${campo('Número da CNH', 'cnhNumero', { mascara: 'cnh', extra: 'inputmode="numeric"' })}
  ${campo('Validade da CNH', 'cnhValidade', { tipo: 'date' })}
  ${campo('Senha', 'senha', { tipo: 'password', classe: 'inteiro', dica: 'Mínimo de 8 caracteres.', extra: 'autocomplete="new-password"' })}
  <div class="form-acoes"><button class="btn btn-primario" type="submit" style="width:100%">Criar conta</button></div>`;

aoEnviar(formCadastro, (d) => {
  const e = {};
  if (!d.nomeCompleto) e.nomeCompleto = 'Informe o nome completo.';
  if (!cpfValido(d.cpf)) e.cpf = 'CPF inválido.';
  if (soDigitos(d.telefone).length < 10) e.telefone = 'Informe o DDD e o telefone.';
  if (!/^\S+@\S+\.\S+$/.test(d.email)) e.email = 'E-mail inválido.';
  if (soDigitos(d.cnhNumero).length !== 11) e.cnhNumero = 'A CNH deve ter 11 dígitos.';
  if (!d.cnhValidade) e.cnhValidade = 'Informe a validade da CNH.';
  if ((d.senha || '').length < 8) e.senha = 'A senha deve ter no mínimo 8 caracteres.';
  return e;
}, async (d) => {
  await api.post('/cadastro', { ...d, cpf: soDigitos(d.cpf), telefone: d.telefone, cnhNumero: soDigitos(d.cnhNumero) });
  toast('Cadastro realizado. Entre com o seu e-mail e a senha.');
  formCadastro.reset();
  document.getElementById('entrar-bloco').hidden = false;
  document.getElementById('cadastro-bloco').hidden = true;
});

function sair() {
  sessao.limpar();
  location.hash = '';
  mostrarLogin();
}

document.addEventListener('sessao-expirada', () => {
  if (usuario) {
    toast('Sessão encerrada. Entre novamente.', 'erro');
    sair();
  }
});

function iniciar() {
  telaLogin.hidden = true;
  telaApp.hidden = false;
  const ehCliente = usuario.perfil === 'CLIENTE';
  const menu = ehCliente ? MENU_PORTAL : MENU_STAFF.filter((m) => !m.so || m.so === usuario.perfil);

  if (ehCliente) {
    telaApp.innerHTML = `<div class="portal-fundo">
      <header class="portal-nav"><div class="logo escuro">${logo()}</div>
        <nav aria-label="Portal do cliente">${menu.map((m) => `<a href="#/${m.rota}" data-rota="${m.rota}">${m.titulo}</a>`).join('')}</nav>
        <div><span class="suave">${esc(usuario.nome.split(' ')[0])}</span> <button class="btn btn-leve btn-pequeno" id="btn-sair">Sair</button></div></header>
      <div class="portal-main" id="conteudo"></div></div>`;
  } else {
    telaApp.innerHTML = `<aside class="lateral" id="lateral" aria-label="Menu principal"><div class="logo">${logo()}</div>
        <nav>${menu.map((m) => `<a href="#/${m.rota}" data-rota="${m.rota}">${m.titulo}</a>`).join('')}</nav>
        <div class="usuario"><strong>${esc(usuario.nome)}</strong><small>${rotulo(usuario.perfil)}</small><button id="btn-sair">Sair</button></div></aside>
      <div class="conteudo-area"><header class="topo"><div><button class="btn btn-sec btn-pequeno menu-btn" id="btn-menu" aria-label="Abrir menu">☰</button>
        <strong>Olá, ${esc(usuario.nome.split(' ')[0])}</strong></div><span class="suave">${new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}</span></header>
        <main id="conteudo"></main></div>`;
    document.getElementById('btn-menu').addEventListener('click', () => document.getElementById('lateral').classList.toggle('aberta'));
  }
  document.getElementById('btn-sair').addEventListener('click', sair);
  window.onhashchange = navegar;
  navegar();
}

function navegar() {
  if (!usuario) return;
  const ehCliente = usuario.perfil === 'CLIENTE';
  const padrao = ehCliente ? 'portal' : 'dashboard';
  let rota = location.hash.replace(/^#\//, '') || padrao;
  const permitidas = ehCliente ? MENU_PORTAL.map((m) => m.rota) : MENU_STAFF.filter((m) => !m.so || m.so === usuario.perfil).map((m) => m.rota);
  if (!permitidas.includes(rota)) {
    location.hash = '#/' + padrao;
    return;
  }
  document.querySelectorAll('[data-rota]').forEach((a) => a.classList.toggle('ativo', a.dataset.rota === rota));
  const lateral = document.getElementById('lateral');
  if (lateral) lateral.classList.remove('aberta');
  const conteudo = document.getElementById('conteudo');
  if (ehCliente) {
    portal.render(conteudo, rota.replace('portal', '').replace('/', '') || 'veiculos', usuario);
  } else {
    MENU_STAFF.find((m) => m.rota === rota).modulo.render(conteudo, usuario);
  }
}

async function iniciarSessao() {
  if (!sessao.obter()) {
    mostrarLogin();
    return;
  }
  try {
    usuario = await api.get('/eu');
    iniciar();
  } catch {
    sessao.limpar();
    mostrarLogin();
  }
}

iniciarSessao();

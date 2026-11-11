import { ErroApi, esc, rotulo } from './api.js';

export function toast(mensagem, tipo = 'sucesso') {
  const el = document.createElement('div');
  el.className = 'toast ' + tipo;
  el.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
  el.textContent = mensagem;
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => el.remove(), tipo === 'erro' ? 6000 : 3500);
}

export const mensagemErro = (e) => (e instanceof ErroApi ? e.message : 'Erro inesperado. Tente novamente.');

const CORES = {
  DISPONIVEL: 'b-verde', RESERVADO: 'b-amarelo', ALUGADO: 'b-azul', MANUTENCAO: 'b-laranja', INATIVO: '',
  PENDENTE: 'b-amarelo', CONFIRMADA: 'b-azul', CANCELADA: 'b-vermelho', CONCLUIDA: 'b-verde',
  ATIVA: 'b-azul', ATRASADA: 'b-vermelho', FINALIZADA: 'b-verde',
  PAGO: 'b-verde', PARCIAL: 'b-roxo', ESTORNADO: '',
  VALIDA: 'b-verde', VENCIDA: 'b-vermelho', INEXISTENTE: 'b-vermelho', IRREGULAR: 'b-laranja', NAO_VERIFICADA: '',
};

export const badge = (status) => `<span class="badge ${CORES[status] || ''}">${esc(rotulo(status))}</span>`;

export function tabela(colunas, linhas, vazio = 'Nenhum registro encontrado.') {
  if (!linhas.length) return `<div class="vazio"><strong>${esc(vazio)}</strong>Ajuste os filtros ou cadastre um novo registro.</div>`;
  const cab = colunas.map((c) => `<th scope="col">${esc(c.titulo)}</th>`).join('');
  const corpo = linhas.map((l) => `<tr>${colunas.map((c) => `<td>${c.celula(l)}</td>`).join('')}</tr>`).join('');
  return `<div class="tabela-area"><table><thead><tr>${cab}</tr></thead><tbody>${corpo}</tbody></table></div>
          <div class="rodape-tabela">${linhas.length} ${linhas.length === 1 ? 'registro' : 'registros'}</div>`;
}

export function campo(rotuloCampo, nome, { tipo = 'text', valor = '', obrigatorio = true, mascara = '', dica = '', extra = '', classe = '' } = {}) {
  return `<label class="campo ${classe}"><span>${esc(rotuloCampo)}${obrigatorio ? '<b class="obrigatorio" aria-hidden="true">*</b>' : ''}</span>
    <input name="${nome}" type="${tipo}" value="${esc(valor)}" ${mascara ? `data-mascara="${mascara}"` : ''} ${extra}>
    ${dica ? `<small class="dica">${esc(dica)}</small>` : ''}<small class="erro-campo" data-erro="${nome}"></small></label>`;
}

export function selecao(rotuloCampo, nome, opcoes, { valor = '', vazio = 'Selecione...', obrigatorio = true, classe = '' } = {}) {
  const itens = opcoes.map((o) => `<option value="${esc(o.valor)}" ${String(o.valor) === String(valor) ? 'selected' : ''}>${esc(o.rotulo)}</option>`).join('');
  return `<label class="campo ${classe}"><span>${esc(rotuloCampo)}${obrigatorio ? '<b class="obrigatorio" aria-hidden="true">*</b>' : ''}</span>
    <select name="${nome}">${vazio === null ? '' : `<option value="">${esc(vazio)}</option>`}${itens}</select>
    <small class="erro-campo" data-erro="${nome}"></small></label>`;
}

export const opcoes = (valores) => valores.map((v) => ({ valor: v, rotulo: rotulo(v) }));

export function lerForm(form) {
  const dados = {};
  new FormData(form).forEach((v, k) => (dados[k] = typeof v === 'string' ? v.trim() : v));
  return dados;
}

export function mostrarErros(form, erros) {
  form.querySelectorAll('[data-erro]').forEach((el) => (el.textContent = ''));
  form.querySelectorAll('.invalido').forEach((el) => el.classList.remove('invalido'));
  Object.entries(erros).forEach(([nome, msg]) => {
    const alvo = form.querySelector(`[data-erro="${nome}"]`);
    if (alvo) alvo.textContent = msg;
    const entrada = form.elements[nome];
    if (entrada) entrada.classList.add('invalido');
  });
}

export function aoEnviar(form, validar, enviar) {
  let ocupado = false;
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    if (ocupado) return;
    const dados = lerForm(form);
    const erros = validar ? validar(dados) : {};
    mostrarErros(form, erros);
    if (Object.keys(erros).length) {
      const primeiro = form.querySelector('.invalido');
      if (primeiro) primeiro.focus();
      return;
    }
    ocupado = true;
    const botao = form.querySelector('button[type=submit]');
    const textoOriginal = botao ? botao.textContent : '';
    if (botao) {
      botao.disabled = true;
      botao.textContent = 'Aguarde...';
    }
    try {
      await enviar(dados);
    } catch (e) {
      if (e instanceof ErroApi && e.campos) mostrarErros(form, e.campos);
      toast(mensagemErro(e), 'erro');
    } finally {
      ocupado = false;
      if (botao && botao.isConnected) {
        botao.disabled = false;
        botao.textContent = textoOriginal;
      }
    }
  });
}

export function abrirModal(titulo, conteudo) {
  const fundo = document.createElement('div');
  fundo.className = 'fundo-modal';
  fundo.innerHTML = `<div class="modal" role="dialog" aria-modal="true" aria-label="${esc(titulo)}">
    <div class="modal-cab"><h2>${esc(titulo)}</h2><button type="button" aria-label="Fechar">&times;</button></div>
    <div class="modal-corpo">${conteudo}</div></div>`;
  const fechar = () => {
    document.removeEventListener('keydown', aoTecla);
    fundo.remove();
  };
  const aoTecla = (e) => e.key === 'Escape' && fechar();
  document.addEventListener('keydown', aoTecla);
  fundo.addEventListener('mousedown', (e) => e.target === fundo && fechar());
  fundo.querySelector('.modal-cab button').addEventListener('click', fechar);
  document.body.appendChild(fundo);
  const primeiro = fundo.querySelector('input, select');
  if (primeiro) primeiro.focus();
  return { el: fundo.querySelector('.modal-corpo'), fechar };
}

export function confirmar(titulo, mensagem, textoBotao = 'Confirmar') {
  return new Promise((resolver) => {
    const m = abrirModal(titulo, `<p>${esc(mensagem)}</p>
      <div class="form-acoes"><button class="btn btn-sec" data-nao>Cancelar</button><button class="btn btn-perigo" data-sim>${esc(textoBotao)}</button></div>`);
    m.el.querySelector('[data-nao]').addEventListener('click', () => { m.fechar(); resolver(false); });
    m.el.querySelector('[data-sim]').addEventListener('click', () => { m.fechar(); resolver(true); });
  });
}

export async function carregar(el, funcao) {
  el.innerHTML = '<div class="carregando">Carregando...</div>';
  try {
    await funcao();
  } catch (e) {
    el.innerHTML = `<div class="erro-bloco" role="alert"><p>${esc(mensagemErro(e))}</p><button class="btn btn-sec" data-tentar>Tentar novamente</button></div>`;
    el.querySelector('[data-tentar]').addEventListener('click', () => carregar(el, funcao));
  }
}

export function cabecalho(titulo, descricao, acaoHtml = '') {
  return `<div class="cabecalho-pagina"><div><h1>${esc(titulo)}</h1><p>${esc(descricao)}</p></div>${acaoHtml}</div>`;
}

export function filtrarTexto(itens, texto, extrair) {
  const t = texto.trim().toLowerCase();
  return t ? itens.filter((i) => extrair(i).toLowerCase().includes(t)) : itens;
}

document.addEventListener('input', (e) => {
  const tipo = e.target.dataset && e.target.dataset.mascara;
  if (!tipo) return;
  let v = e.target.value.replace(/\D/g, '');
  if (tipo === 'cpf') {
    v = v.slice(0, 11).replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  } else if (tipo === 'telefone') {
    v = v.slice(0, 11);
    if (v.length > 10) v = `(${v.slice(0, 2)}) ${v.slice(2, 7)}-${v.slice(7)}`;
    else if (v.length > 6) v = `(${v.slice(0, 2)}) ${v.slice(2, 6)}-${v.slice(6)}`;
    else if (v.length > 2) v = `(${v.slice(0, 2)}) ${v.slice(2)}`;
    else if (v.length > 0) v = `(${v}`;
  } else if (tipo === 'cnh') {
    v = v.slice(0, 11);
  }
  e.target.value = v;
});

export function cpfValido(cpf) {
  const d = (cpf || '').replace(/\D/g, '');
  if (d.length !== 11 || /^(\d)\1+$/.test(d)) return false;
  const dv = (n) => {
    let s = 0;
    for (let i = 0; i < n; i++) s += Number(d[i]) * (n + 1 - i);
    const r = (s * 10) % 11;
    return r === 10 ? 0 : r;
  };
  return dv(9) === Number(d[9]) && dv(10) === Number(d[10]);
}

import { api, cpfMask, data, esc, soDigitos } from '../api.js';
import { abrirModal, aoEnviar, badge, cabecalho, campo, carregar, confirmar, cpfValido, filtrarTexto, tabela, toast } from '../ui.js';

export function formularioCliente(d, { alteraCpf = true } = {}) {
  return `
    ${campo('Nome completo', 'nomeCompleto', { valor: d.nomeCompleto, classe: 'inteiro' })}
    ${alteraCpf ? campo('CPF', 'cpf', { valor: cpfMask(d.cpf), mascara: 'cpf', extra: 'inputmode="numeric" placeholder="000.000.000-00"' })
      : campo('CPF', 'cpf', { valor: cpfMask(d.cpf), obrigatorio: false, extra: 'readonly', dica: 'O CPF não pode ser alterado.' })}
    ${campo('Telefone', 'telefone', { valor: d.telefone, mascara: 'telefone', extra: 'placeholder="(11) 90000-0000"' })}
    ${campo('E-mail', 'email', { tipo: 'email', valor: d.email, classe: 'inteiro' })}
    ${campo('Número da CNH', 'cnhNumero', { valor: d.cnhNumero, mascara: 'cnh', extra: 'inputmode="numeric"' })}
    ${campo('Validade da CNH', 'cnhValidade', { tipo: 'date', valor: d.cnhValidade })}`;
}

export function validarCliente(x, { alteraCpf = true } = {}) {
  const e = {};
  if (!x.nomeCompleto) e.nomeCompleto = 'Informe o nome completo.';
  if (alteraCpf && !cpfValido(x.cpf)) e.cpf = 'CPF inválido.';
  if (soDigitos(x.telefone).length < 10) e.telefone = 'Informe o DDD e o telefone.';
  if (!/^\S+@\S+\.\S+$/.test(x.email)) e.email = 'E-mail inválido.';
  if (soDigitos(x.cnhNumero).length !== 11) e.cnhNumero = 'A CNH deve ter 11 dígitos.';
  if (!x.cnhValidade) e.cnhValidade = 'Informe a validade da CNH.';
  return e;
}

export function corpoCliente(x) {
  return { ...x, cpf: soDigitos(x.cpf), cnhNumero: soDigitos(x.cnhNumero) };
}

export function render(el, usuario) {
  const admin = usuario.perfil === 'ADMIN';
  let lista = [];
  const vazio = { nomeCompleto: '', cpf: '', email: '', telefone: '', cnhNumero: '', cnhValidade: '' };

  const colunas = [
    { titulo: 'Cliente', celula: (c) => `<strong>${esc(c.nomeCompleto)}</strong><div class="suave">${esc(c.email)}</div>` },
    { titulo: 'CPF', celula: (c) => cpfMask(c.cpf) },
    { titulo: 'Telefone', celula: (c) => esc(c.telefone) },
    { titulo: 'CNH', celula: (c) => `<span class="mono">${esc(c.cnhNumero)}</span><div class="suave">até ${data(c.cnhValidade)}</div>` },
    { titulo: 'Situação CNH', celula: (c) => badge(c.cnhSituacao) },
    { titulo: 'Ações', celula: (c) => `<div class="acoes"><button class="btn btn-sec btn-pequeno" data-editar="${c.id}">Editar</button>${admin ? `<button class="btn btn-leve btn-pequeno" data-excluir="${c.id}">Excluir</button>` : ''}</div>` },
  ];

  function desenhar() {
    const itens = filtrarTexto(lista, el.querySelector('#f-busca').value, (c) => `${c.nomeCompleto} ${c.cpf} ${c.email}`);
    el.querySelector('#tabela').innerHTML = tabela(colunas, itens, 'Nenhum cliente encontrado.');
  }

  async function recarregar() {
    lista = await api.get('/clientes');
    desenhar();
  }

  function abrirForm(c) {
    const m = abrirModal(c ? 'Editar cliente' : 'Novo cliente', `<form class="form-grade" novalidate>${formularioCliente(c || vazio)}
      <p class="dica inteiro">A CNH é validada automaticamente ao salvar.</p>
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">${c ? 'Salvar alterações' : 'Cadastrar'}</button></div></form>`);
    const form = m.el.querySelector('form');
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    aoEnviar(form, (x) => validarCliente(x), async (x) => {
      const salvo = c ? await api.put('/clientes/' + c.id, corpoCliente(x)) : await api.post('/clientes', corpoCliente(x));
      toast(`${c ? 'Cliente atualizado' : 'Cliente cadastrado'}. CNH: ${salvo.cnhSituacao === 'VALIDA' ? 'válida' : salvo.cnhSituacao.toLowerCase()}.`);
      m.fechar();
      await recarregar();
    });
  }

  el.innerHTML = cabecalho('Clientes', 'Cadastro e consulta de clientes.', '<button class="btn btn-primario" id="novo">+ Novo cliente</button>') + `
    <section class="card"><div class="filtros"><input id="f-busca" placeholder="Buscar por nome, CPF ou e-mail" aria-label="Buscar cliente"></div><div id="tabela"></div></section>`;
  el.querySelector('#f-busca').addEventListener('input', desenhar);
  el.querySelector('#novo').addEventListener('click', () => abrirForm(null));
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const editar = e.target.closest('[data-editar]');
    const excluir = e.target.closest('[data-excluir]');
    if (editar) abrirForm(lista.find((c) => c.id === Number(editar.dataset.editar)));
    if (excluir) {
      const c = lista.find((x) => x.id === Number(excluir.dataset.excluir));
      if (await confirmar('Excluir cliente', `Deseja excluir ${c.nomeCompleto}? Clientes com reservas ou locações não podem ser excluídos.`, 'Excluir')) {
        try {
          await api.del('/clientes/' + c.id);
          toast('Cliente excluído.');
          await recarregar();
        } catch (err) {
          toast(err.message, 'erro');
        }
      }
    }
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

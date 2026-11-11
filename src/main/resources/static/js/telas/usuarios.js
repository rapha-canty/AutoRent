import { api, esc, rotulo } from '../api.js';
import { abrirModal, aoEnviar, cabecalho, campo, carregar, opcoes, selecao, tabela, toast } from '../ui.js';

export function render(el, usuario) {
  let lista = [];

  const colunas = [
    { titulo: 'Nome', celula: (u) => `<strong>${esc(u.nome)}</strong>` },
    { titulo: 'E-mail', celula: (u) => esc(u.email) },
    { titulo: 'Perfil', celula: (u) => rotulo(u.perfil) },
    { titulo: 'Situação', celula: (u) => `<span class="badge ${u.ativo ? 'b-verde' : ''}">${u.ativo ? 'Ativo' : 'Inativo'}</span>` },
    { titulo: 'Ações', celula: (u) => (u.id === usuario.id ? '' : `<div class="acoes"><button class="btn btn-sec btn-pequeno" data-alternar="${u.id}">${u.ativo ? 'Desativar' : 'Ativar'}</button></div>`) },
  ];

  async function recarregar() {
    lista = await api.get('/usuarios');
    el.querySelector('#tabela').innerHTML = tabela(colunas, lista, 'Nenhum usuário encontrado.');
  }

  function novo() {
    const m = abrirModal('Novo usuário', `<form class="form-grade" novalidate>
      ${campo('Nome', 'nome', { classe: 'inteiro' })}
      ${campo('E-mail', 'email', { tipo: 'email', classe: 'inteiro' })}
      ${campo('Senha', 'senha', { tipo: 'password', dica: 'Mínimo de 8 caracteres.', extra: 'autocomplete="new-password"' })}
      ${selecao('Perfil', 'perfil', opcoes(['ADMIN', 'VENDEDOR']), { vazio: null })}
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">Criar usuário</button></div></form>`);
    const form = m.el.querySelector('form');
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    aoEnviar(form, (x) => {
      const e = {};
      if (!x.nome) e.nome = 'Informe o nome.';
      if (!/^\S+@\S+\.\S+$/.test(x.email)) e.email = 'E-mail inválido.';
      if ((x.senha || '').length < 8) e.senha = 'A senha deve ter no mínimo 8 caracteres.';
      return e;
    }, async (x) => {
      await api.post('/usuarios', x);
      toast('Usuário criado.');
      m.fechar();
      await recarregar();
    });
  }

  el.innerHTML = cabecalho('Usuários', 'Gerencie o acesso ao sistema (administradores e vendedores).', '<button class="btn btn-primario" id="novo">+ Novo usuário</button>') + '<section class="card"><div id="tabela"></div></section>';
  el.querySelector('#novo').addEventListener('click', novo);
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const b = e.target.closest('[data-alternar]');
    if (!b) return;
    const u = lista.find((x) => x.id === Number(b.dataset.alternar));
    try {
      await api.patch(`/usuarios/${u.id}/ativo?valor=${!u.ativo}`);
      toast(u.ativo ? 'Usuário desativado.' : 'Usuário ativado.');
      await recarregar();
    } catch (err) {
      toast(err.message, 'erro');
    }
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

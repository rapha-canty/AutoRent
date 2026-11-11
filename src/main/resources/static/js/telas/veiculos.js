import { api, esc, moeda } from '../api.js';
import { abrirModal, aoEnviar, badge, cabecalho, campo, carregar, confirmar, filtrarTexto, opcoes, selecao, tabela, toast } from '../ui.js';

const CATEGORIAS = opcoes(['ECONOMICO', 'COMPACTO', 'SEDAN', 'SUV', 'PICAPE', 'LUXO']);
const STATUS = opcoes(['DISPONIVEL', 'RESERVADO', 'ALUGADO', 'MANUTENCAO', 'INATIVO']);
const STATUS_MANUAL = opcoes(['DISPONIVEL', 'MANUTENCAO', 'INATIVO']);
const PLACA = /^[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}$/;

export function render(el, usuario) {
  const admin = usuario.perfil === 'ADMIN';
  let lista = [];

  const colunas = [
    { titulo: 'Veículo', celula: (v) => `<strong>${esc(v.marca)} ${esc(v.modelo)}</strong><div class="suave">${v.ano} · ${esc(v.cor)}</div>` },
    { titulo: 'Placa', celula: (v) => `<span class="mono">${esc(v.placa)}</span>` },
    { titulo: 'Categoria', celula: (v) => CATEGORIAS.find((c) => c.valor === v.categoria).rotulo },
    { titulo: 'Km', celula: (v) => v.quilometragem.toLocaleString('pt-BR') },
    { titulo: 'Diária', celula: (v) => `<b>${moeda(v.valorDiaria)}</b>` },
    { titulo: 'Status', celula: (v) => badge(v.status) },
  ];
  if (admin) {
    colunas.push({ titulo: 'Ações', celula: (v) => `<div class="acoes"><button class="btn btn-sec btn-pequeno" data-editar="${v.id}">Editar</button><button class="btn btn-leve btn-pequeno" data-excluir="${v.id}">Excluir</button></div>` });
  }

  function desenhar() {
    const busca = el.querySelector('#f-busca').value;
    const cat = el.querySelector('#f-cat').value;
    const st = el.querySelector('#f-status').value;
    let itens = filtrarTexto(lista, busca, (v) => `${v.marca} ${v.modelo} ${v.placa}`);
    if (cat) itens = itens.filter((v) => v.categoria === cat);
    if (st) itens = itens.filter((v) => v.status === st);
    el.querySelector('#tabela').innerHTML = tabela(colunas, itens, 'Nenhum veículo encontrado.');
  }

  function formulario(v) {
    const d = v || { placa: '', marca: '', modelo: '', ano: new Date().getFullYear(), cor: '', categoria: '', quilometragem: 0, valorDiaria: '', status: 'DISPONIVEL' };
    const travado = v && (v.status === 'ALUGADO' || v.status === 'RESERVADO');
    const m = abrirModal(v ? 'Editar veículo' : 'Novo veículo', `<form class="form-grade" novalidate>
      ${campo('Placa', 'placa', { valor: d.placa, extra: 'placeholder="ABC1D23" maxlength="8"' })}
      ${selecao('Categoria', 'categoria', CATEGORIAS, { valor: d.categoria })}
      ${campo('Marca', 'marca', { valor: d.marca })}
      ${campo('Modelo', 'modelo', { valor: d.modelo })}
      ${campo('Ano', 'ano', { tipo: 'number', valor: d.ano })}
      ${campo('Cor', 'cor', { valor: d.cor })}
      ${campo('Quilometragem', 'quilometragem', { tipo: 'number', valor: d.quilometragem, extra: 'min="0"' })}
      ${campo('Valor da diária (R$)', 'valorDiaria', { tipo: 'number', valor: d.valorDiaria, extra: 'min="0" step="0.01"' })}
      ${travado ? `<p class="aviso inteiro">Status atual: <b>${v.status === 'ALUGADO' ? 'Alugado' : 'Reservado'}</b> — controlado automaticamente.</p>`
        : selecao('Status', 'status', STATUS_MANUAL, { valor: d.status, vazio: null, classe: 'inteiro' })}
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">${v ? 'Salvar alterações' : 'Cadastrar'}</button></div>
    </form>`);
    const form = m.el.querySelector('form');
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    aoEnviar(form, (x) => {
      const e = {};
      if (!PLACA.test(x.placa.replace(/[^A-Za-z0-9]/g, ''))) e.placa = 'Placa inválida (ABC1234 ou ABC1D23).';
      if (!x.categoria) e.categoria = 'Selecione a categoria.';
      if (!x.marca) e.marca = 'Informe a marca.';
      if (!x.modelo) e.modelo = 'Informe o modelo.';
      if (!(Number(x.ano) >= 1990 && Number(x.ano) <= 2100)) e.ano = 'Ano inválido.';
      if (!x.cor) e.cor = 'Informe a cor.';
      if (x.quilometragem === '' || Number(x.quilometragem) < 0) e.quilometragem = 'Quilometragem inválida.';
      if (!(Number(x.valorDiaria) > 0)) e.valorDiaria = 'A diária deve ser maior que zero.';
      return e;
    }, async (x) => {
      const corpo = { ...x, ano: Number(x.ano), quilometragem: Number(x.quilometragem), valorDiaria: Number(x.valorDiaria), status: travado ? null : x.status };
      if (v) await api.put('/veiculos/' + v.id, corpo);
      else await api.post('/veiculos', corpo);
      toast(v ? 'Veículo atualizado.' : 'Veículo cadastrado.');
      m.fechar();
      await recarregar();
    });
  }

  async function recarregar() {
    lista = await api.get('/veiculos');
    desenhar();
  }

  el.innerHTML = cabecalho('Veículos', 'Frota cadastrada e disponibilidade.', admin ? '<button class="btn btn-primario" id="novo">+ Novo veículo</button>' : '') + `
    <section class="card"><div class="filtros">
      <input id="f-busca" placeholder="Buscar por marca, modelo ou placa" aria-label="Buscar veículo">
      <select id="f-cat" aria-label="Categoria"><option value="">Todas as categorias</option>${CATEGORIAS.map((c) => `<option value="${c.valor}">${c.rotulo}</option>`).join('')}</select>
      <select id="f-status" aria-label="Status"><option value="">Todos os status</option>${STATUS.map((c) => `<option value="${c.valor}">${c.rotulo}</option>`).join('')}</select>
    </div><div id="tabela"></div></section>`;

  el.querySelectorAll('.filtros input, .filtros select').forEach((c) => c.addEventListener('input', desenhar));
  if (admin) el.querySelector('#novo').addEventListener('click', () => formulario(null));
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const editar = e.target.closest('[data-editar]');
    const excluir = e.target.closest('[data-excluir]');
    if (editar) formulario(lista.find((v) => v.id === Number(editar.dataset.editar)));
    if (excluir) {
      const v = lista.find((x) => x.id === Number(excluir.dataset.excluir));
      if (await confirmar('Excluir veículo', `Deseja excluir ${v.marca} ${v.modelo} (${v.placa})? Veículos com histórico serão apenas desativados.`, 'Excluir')) {
        try {
          const r = await api.del('/veiculos/' + v.id);
          toast(r.mensagem);
          await recarregar();
        } catch (err) {
          toast(err.message, 'erro');
        }
      }
    }
  });

  carregar(el.querySelector('#tabela'), recarregar);
}

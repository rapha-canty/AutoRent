import { api, dataHora, esc, moeda, paraApiDataHora, paraInputDataHora } from '../api.js';
import { abrirModal, aoEnviar, badge, cabecalho, campo, carregar, confirmar, filtrarTexto, opcoes, selecao, tabela, toast } from '../ui.js';

const STATUS = opcoes(['PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA']);

export async function novaReserva({ atendimento, idVeiculo = '', inicio = '', fim = '', aoCriar }) {
  const padrao = new Date(Date.now() + 24 * 3600 * 1000);
  padrao.setMinutes(0, 0, 0);
  const ini = inicio || paraInputDataHora(padrao);
  const fi = fim || paraInputDataHora(new Date(padrao.getTime() + 3 * 24 * 3600 * 1000));
  const clientes = atendimento ? await api.get('/clientes') : [];

  const m = abrirModal('Nova reserva', `<form class="form-grade" novalidate>
    ${atendimento ? selecao('Cliente', 'idCliente', clientes.map((c) => ({ valor: c.id, rotulo: c.nomeCompleto })), { classe: 'inteiro' }) : ''}
    ${campo('Retirada', 'dataInicio', { tipo: 'datetime-local', valor: ini })}
    ${campo('Devolução', 'dataFim', { tipo: 'datetime-local', valor: fi })}
    <label class="campo inteiro"><span>Veículo disponível no período<b class="obrigatorio" aria-hidden="true">*</b></span><select name="idVeiculo"></select><small class="erro-campo" data-erro="idVeiculo"></small></label>
    <div id="estimativa" class="aviso inteiro" hidden aria-live="polite"></div>
    <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">Reservar</button></div>
  </form>`);
  const form = m.el.querySelector('form');
  form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
  let veiculos = [];

  async function atualizarVeiculos() {
    const sel = form.elements.idVeiculo;
    const atual = sel.value || String(idVeiculo);
    const i = form.elements.dataInicio.value;
    const f = form.elements.dataFim.value;
    if (!i || !f || new Date(f) <= new Date(i)) {
      sel.innerHTML = '<option value="">Informe um período válido</option>';
      veiculos = [];
      estimativa();
      return;
    }
    try {
      veiculos = await api.get(`/veiculos/disponiveis?inicio=${encodeURIComponent(paraApiDataHora(i))}&fim=${encodeURIComponent(paraApiDataHora(f))}`);
    } catch {
      veiculos = [];
    }
    sel.innerHTML = veiculos.length
      ? '<option value="">Selecione...</option>' + veiculos.map((v) => `<option value="${v.id}" ${String(v.id) === atual ? 'selected' : ''}>${esc(v.marca + ' ' + v.modelo)} · ${esc(v.placa)} · ${moeda(v.valorDiaria)}/dia</option>`).join('')
      : '<option value="">Nenhum veículo disponível neste período</option>';
    estimativa();
  }

  function estimativa() {
    const caixa = m.el.querySelector('#estimativa');
    const v = veiculos.find((x) => String(x.id) === form.elements.idVeiculo.value);
    if (!v) {
      caixa.hidden = true;
      return;
    }
    const minutos = (new Date(form.elements.dataFim.value) - new Date(form.elements.dataInicio.value)) / 60000;
    const diarias = Math.max(1, Math.ceil(minutos / 1440));
    caixa.hidden = false;
    caixa.innerHTML = `${diarias} ${diarias === 1 ? 'diária' : 'diárias'} × ${moeda(v.valorDiaria)} = <b>${moeda(diarias * v.valorDiaria)}</b> (estimativa; o valor final é calculado pelo sistema)`;
  }

  form.elements.dataInicio.addEventListener('change', atualizarVeiculos);
  form.elements.dataFim.addEventListener('change', atualizarVeiculos);
  form.elements.idVeiculo.addEventListener('change', estimativa);
  await atualizarVeiculos();

  aoEnviar(form, (x) => {
    const e = {};
    if (atendimento && !x.idCliente) e.idCliente = 'Selecione o cliente.';
    if (!x.dataInicio) e.dataInicio = 'Informe a retirada.';
    if (!x.dataFim) e.dataFim = 'Informe a devolução.';
    else if (x.dataInicio && new Date(x.dataFim) <= new Date(x.dataInicio)) e.dataFim = 'A devolução deve ser posterior à retirada.';
    if (!x.idVeiculo) e.idVeiculo = 'Selecione o veículo.';
    return e;
  }, async (x) => {
    await api.post('/reservas', {
      idCliente: atendimento ? Number(x.idCliente) : null,
      idVeiculo: Number(x.idVeiculo),
      dataInicio: paraApiDataHora(x.dataInicio),
      dataFim: paraApiDataHora(x.dataFim),
    });
    toast('Reserva criada com sucesso.');
    m.fechar();
    aoCriar();
  });
}

export function render(el) {
  let lista = [];

  const colunas = [
    { titulo: '#', celula: (r) => `<span class="suave">${r.id}</span>` },
    { titulo: 'Cliente', celula: (r) => `<strong>${esc(r.cliente.nomeCompleto)}</strong>` },
    { titulo: 'Veículo', celula: (r) => `${esc(r.veiculo.marca + ' ' + r.veiculo.modelo)}<div class="mono suave">${esc(r.veiculo.placa)}</div>` },
    { titulo: 'Período', celula: (r) => `<div class="suave">${dataHora(r.dataInicio)}<br>até ${dataHora(r.dataFim)}</div>` },
    { titulo: 'Valor previsto', celula: (r) => `<b>${moeda(r.valorTotalPrevisto)}</b>` },
    { titulo: 'Status', celula: (r) => badge(r.status) },
    { titulo: 'Ações', celula: (r) => `<div class="acoes">${r.status === 'PENDENTE' ? `<button class="btn btn-sec btn-pequeno" data-confirmar="${r.id}">Confirmar</button>` : ''}${r.status === 'PENDENTE' || r.status === 'CONFIRMADA' ? `<button class="btn btn-leve btn-pequeno" data-cancelar="${r.id}">Cancelar</button>` : ''}</div>` },
  ];

  function desenhar() {
    let itens = filtrarTexto(lista, el.querySelector('#f-busca').value, (r) => `${r.cliente.nomeCompleto} ${r.veiculo.modelo} ${r.veiculo.placa}`);
    const st = el.querySelector('#f-status').value;
    if (st) itens = itens.filter((r) => r.status === st);
    el.querySelector('#tabela').innerHTML = tabela(colunas, itens, 'Nenhuma reserva encontrada.');
  }

  async function recarregar() {
    lista = await api.get('/reservas');
    desenhar();
  }

  el.innerHTML = cabecalho('Reservas', 'Crie, confirme e acompanhe reservas.', '<button class="btn btn-primario" id="nova">+ Nova reserva</button>') + `
    <section class="card"><div class="filtros"><input id="f-busca" placeholder="Buscar por cliente, modelo ou placa" aria-label="Buscar reserva">
    <select id="f-status" aria-label="Status"><option value="">Todos os status</option>${STATUS.map((s) => `<option value="${s.valor}">${s.rotulo}</option>`).join('')}</select></div><div id="tabela"></div></section>`;
  el.querySelectorAll('.filtros input, .filtros select').forEach((c) => c.addEventListener('input', desenhar));
  el.querySelector('#nova').addEventListener('click', () => novaReserva({ atendimento: true, aoCriar: recarregar }));
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const conf = e.target.closest('[data-confirmar]');
    const canc = e.target.closest('[data-cancelar]');
    try {
      if (conf) {
        await api.patch(`/reservas/${conf.dataset.confirmar}/confirmar`);
        toast('Reserva confirmada.');
        await recarregar();
      }
      if (canc && await confirmar('Cancelar reserva', `Deseja cancelar a reserva #${canc.dataset.cancelar}? Esta ação não pode ser desfeita.`, 'Cancelar reserva')) {
        await api.patch(`/reservas/${canc.dataset.cancelar}/cancelar`);
        toast('Reserva cancelada.');
        await recarregar();
      }
    } catch (err) {
      toast(err.message, 'erro');
    }
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

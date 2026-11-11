import { api, dataHora, esc, moeda, paraApiDataHora, paraInputDataHora } from '../api.js';
import { abrirModal, aoEnviar, badge, cabecalho, campo, carregar, filtrarTexto, opcoes, selecao, tabela, toast } from '../ui.js';

const STATUS = opcoes(['ATIVA', 'ATRASADA', 'FINALIZADA']);

export function render(el) {
  let lista = [];

  const colunas = [
    { titulo: '#', celula: (l) => `<span class="suave">${l.id}</span>` },
    { titulo: 'Cliente', celula: (l) => `<strong>${esc(l.cliente.nomeCompleto)}</strong>` },
    { titulo: 'Veículo', celula: (l) => `${esc(l.veiculo.marca + ' ' + l.veiculo.modelo)}<div class="mono suave">${esc(l.veiculo.placa)}</div>` },
    { titulo: 'Retirada', celula: (l) => `<div class="suave">${dataHora(l.dataRetirada)}<br>${l.kmRetirada.toLocaleString('pt-BR')} km</div>` },
    { titulo: 'Devolução', celula: (l) => `<div class="suave">Prev.: ${dataHora(l.dataPrevistaDevolucao)}${l.dataDevolucaoEfetiva ? `<br>Efet.: ${dataHora(l.dataDevolucaoEfetiva)} · ${l.kmDevolucao.toLocaleString('pt-BR')} km` : ''}</div>` },
    { titulo: 'Valor', celula: (l) => `<b>${moeda(l.valorDevido)}</b>${l.multaAcumulada || l.valorMulta > 0 ? `<div class="suave" style="color:#be123c">multa ${moeda(l.multaAcumulada || l.valorMulta)}</div>` : ''}` },
    { titulo: 'Status', celula: (l) => badge(l.status) },
    { titulo: 'Ações', celula: (l) => (l.status !== 'FINALIZADA' ? `<div class="acoes"><button class="btn btn-sec btn-pequeno" data-devolver="${l.id}">Devolução</button></div>` : '') },
  ];

  function desenhar() {
    let itens = filtrarTexto(lista, el.querySelector('#f-busca').value, (l) => `${l.cliente.nomeCompleto} ${l.veiculo.modelo} ${l.veiculo.placa}`);
    const st = el.querySelector('#f-status').value;
    if (st) itens = itens.filter((l) => l.status === st);
    el.querySelector('#tabela').innerHTML = tabela(colunas, itens, 'Nenhuma locação encontrada.');
  }

  async function recarregar() {
    lista = await api.get('/locacoes');
    desenhar();
  }

  async function retirada() {
    const [reservas, clientes, veiculos] = await Promise.all([api.get('/reservas'), api.get('/clientes'), api.get('/veiculos')]);
    const confirmadas = reservas.filter((r) => r.status === 'CONFIRMADA');
    const amanha = paraInputDataHora(new Date(Date.now() + 24 * 3600 * 1000));
    const m = abrirModal('Registrar retirada', `<form class="form-grade" novalidate>
      <div class="inteiro" style="display:flex;gap:8px"><button type="button" class="btn btn-primario" data-modo="reserva" style="flex:1">A partir de reserva</button><button type="button" class="btn btn-sec" data-modo="balcao" style="flex:1">Locação de balcão</button></div>
      <div class="inteiro" data-bloco="reserva">${selecao('Reserva confirmada', 'idReserva', confirmadas.map((r) => ({ valor: r.id, rotulo: `#${r.id} · ${r.cliente.nomeCompleto} · ${r.veiculo.modelo} · ${dataHora(r.dataInicio)}` })), { vazio: confirmadas.length ? 'Selecione...' : 'Nenhuma reserva confirmada' })}</div>
      <div class="inteiro form-grade" data-bloco="balcao" hidden>
        ${selecao('Cliente', 'idCliente', clientes.map((c) => ({ valor: c.id, rotulo: c.nomeCompleto })), { classe: 'inteiro' })}
        ${selecao('Veículo', 'idVeiculo', veiculos.filter((v) => v.status === 'DISPONIVEL' || v.status === 'RESERVADO').map((v) => ({ valor: v.id, rotulo: `${v.marca} ${v.modelo} · ${v.placa} · ${moeda(v.valorDiaria)}/dia` })), { classe: 'inteiro' })}
        ${campo('Devolução prevista', 'dataPrevista', { tipo: 'datetime-local', valor: amanha, classe: 'inteiro' })}
      </div>
      ${campo('Quilometragem de retirada', 'kmRetirada', { tipo: 'number', obrigatorio: false, extra: 'min="0"', dica: 'Em branco: usa a quilometragem atual do veículo.', classe: 'inteiro' })}
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">Registrar retirada</button></div></form>`);
    const form = m.el.querySelector('form');
    let modo = 'reserva';
    form.querySelectorAll('[data-modo]').forEach((b) => b.addEventListener('click', () => {
      modo = b.dataset.modo;
      form.querySelectorAll('[data-modo]').forEach((x) => (x.className = 'btn ' + (x.dataset.modo === modo ? 'btn-primario' : 'btn-sec')));
      form.querySelectorAll('[data-bloco]').forEach((x) => (x.hidden = x.dataset.bloco !== modo));
    }));
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    aoEnviar(form, (x) => {
      const e = {};
      if (modo === 'reserva') {
        if (!x.idReserva) e.idReserva = 'Selecione a reserva confirmada.';
      } else {
        if (!x.idCliente) e.idCliente = 'Selecione o cliente.';
        if (!x.idVeiculo) e.idVeiculo = 'Selecione o veículo.';
        if (!x.dataPrevista || new Date(x.dataPrevista) <= new Date()) e.dataPrevista = 'Informe uma devolução futura.';
      }
      if (x.kmRetirada !== '' && Number(x.kmRetirada) < 0) e.kmRetirada = 'Quilometragem inválida.';
      return e;
    }, async (x) => {
      const corpo = modo === 'reserva'
        ? { idReserva: Number(x.idReserva) }
        : { idCliente: Number(x.idCliente), idVeiculo: Number(x.idVeiculo), dataPrevistaDevolucao: paraApiDataHora(x.dataPrevista) };
      if (x.kmRetirada !== '') corpo.kmRetirada = Number(x.kmRetirada);
      await api.post('/locacoes/retirada', corpo);
      toast('Retirada registrada.');
      m.fechar();
      await recarregar();
    });
  }

  function devolucao(l) {
    const m = abrirModal('Registrar devolução', `<p>${esc(l.veiculo.marca + ' ' + l.veiculo.modelo)} (${esc(l.veiculo.placa)}) · ${esc(l.cliente.nomeCompleto)}<br><span class="suave">Devolução prevista: ${dataHora(l.dataPrevistaDevolucao)}</span></p>
      <form class="form-grade" novalidate>${campo('Quilometragem de devolução', 'kmDevolucao', { tipo: 'number', valor: l.kmRetirada, classe: 'inteiro', extra: `min="${l.kmRetirada}"`, dica: `Retirada: ${l.kmRetirada.toLocaleString('pt-BR')} km` })}
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">Registrar devolução</button></div></form>`);
    const form = m.el.querySelector('form');
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    aoEnviar(form, (x) => (x.kmDevolucao === '' || Number(x.kmDevolucao) < l.kmRetirada ? { kmDevolucao: `Informe um valor maior ou igual a ${l.kmRetirada}.` } : {}), async (x) => {
      const r = await api.post(`/locacoes/${l.id}/devolucao`, { kmDevolucao: Number(x.kmDevolucao) });
      toast(r.valorMulta > 0 ? `Devolução registrada. Multa por atraso: ${moeda(r.valorMulta)}.` : 'Devolução registrada.');
      m.fechar();
      await recarregar();
    });
  }

  el.innerHTML = cabecalho('Locações', 'Retiradas, devoluções e acompanhamento de atrasos.', '<button class="btn btn-primario" id="retirada">Registrar retirada</button>') + `
    <section class="card"><div class="filtros"><input id="f-busca" placeholder="Buscar por cliente, modelo ou placa" aria-label="Buscar locação">
    <select id="f-status" aria-label="Status"><option value="">Todos os status</option>${STATUS.map((s) => `<option value="${s.valor}">${s.rotulo}</option>`).join('')}</select></div><div id="tabela"></div></section>`;
  el.querySelectorAll('.filtros input, .filtros select').forEach((c) => c.addEventListener('input', desenhar));
  el.querySelector('#retirada').addEventListener('click', () => retirada().catch((e) => toast(e.message, 'erro')));
  el.querySelector('#tabela').addEventListener('click', (e) => {
    const b = e.target.closest('[data-devolver]');
    if (b) devolucao(lista.find((l) => l.id === Number(b.dataset.devolver)));
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

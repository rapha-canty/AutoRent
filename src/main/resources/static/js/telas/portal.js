import { api, dataHora, esc, moeda, paraApiDataHora, paraInputDataHora, rotulo } from '../api.js';
import { aoEnviar, badge, cabecalho, campo, carregar, confirmar, opcoes, selecao, tabela, toast, abrirModal } from '../ui.js';
import { corpoCliente, formularioCliente, validarCliente } from './clientes.js';
import { novaReserva } from './reservas.js';

const CATEGORIAS = opcoes(['ECONOMICO', 'COMPACTO', 'SEDAN', 'SUV', 'PICAPE', 'LUXO']);

export function render(el, secao, usuario) {
  const secoes = { veiculos, reservas, locacoes, pagamentos, perfil };
  (secoes[secao] || veiculos)(el, usuario);
}

function veiculos(el) {
  const ini = new Date(Date.now() + 24 * 3600 * 1000);
  ini.setMinutes(0, 0, 0);
  let filtro = { inicio: paraInputDataHora(ini), fim: paraInputDataHora(new Date(ini.getTime() + 3 * 24 * 3600 * 1000)), categoria: '', busca: '', precoMin: '', precoMax: '' };

  el.innerHTML = `<div class="banner"><h1>Encontre o carro ideal</h1><p>Escolha o período e veja só os veículos realmente disponíveis.</p></div>
    <form id="busca" class="card card-corpo form-grade" novalidate style="grid-template-columns:repeat(auto-fit,minmax(200px,1fr));margin-bottom:24px">
      ${campo('Retirada', 'inicio', { tipo: 'datetime-local', valor: filtro.inicio })}
      ${campo('Devolução', 'fim', { tipo: 'datetime-local', valor: filtro.fim })}
      ${selecao('Categoria', 'categoria', CATEGORIAS, { vazio: 'Todas', obrigatorio: false })}
      ${campo('Modelo ou marca', 'busca', { obrigatorio: false, extra: 'placeholder="Ex.: Corolla"' })}
      ${campo('Diária mínima (R$)', 'precoMin', { tipo: 'number', obrigatorio: false, extra: 'min="0"' })}
      ${campo('Diária máxima (R$)', 'precoMax', { tipo: 'number', obrigatorio: false, extra: 'min="0"' })}
      <div style="display:flex;align-items:flex-end"><button class="btn btn-primario" type="submit">Buscar veículos</button></div>
    </form><div id="resultado"></div>`;

  async function buscar() {
    const q = new URLSearchParams({ inicio: paraApiDataHora(filtro.inicio), fim: paraApiDataHora(filtro.fim) });
    ['categoria', 'busca', 'precoMin', 'precoMax'].forEach((k) => filtro[k] !== '' && q.set(k, filtro[k]));
    const lista = await api.get('/veiculos/disponiveis?' + q);
    const diarias = Math.max(1, Math.ceil((new Date(filtro.fim) - new Date(filtro.inicio)) / 60000 / 1440));
    const area = el.querySelector('#resultado');
    area.innerHTML = lista.length
      ? `<div class="cartoes">${lista.map((v) => `<article class="card cartao-veiculo"><div class="foto">${esc(v.marca[0] + v.modelo[0])}</div><div class="info">
          <small style="color:var(--roxo);font-weight:700;text-transform:uppercase">${rotulo(v.categoria)}</small>
          <h2>${esc(v.marca)} ${esc(v.modelo)}</h2><div class="suave">${v.ano} · ${esc(v.cor)} · ${v.quilometragem.toLocaleString('pt-BR')} km</div>
          <div class="preco">${moeda(v.valorDiaria)}<small class="suave"> /dia</small></div>
          <div class="suave">Total estimado: <b>${moeda(v.valorDiaria * diarias)}</b> (${diarias} ${diarias === 1 ? 'diária' : 'diárias'})</div>
          <button class="btn btn-primario" style="width:100%;margin-top:14px" data-reservar="${v.id}">Reservar</button></div></article>`).join('')}</div>`
      : '<div class="vazio"><strong>Nenhum veículo disponível</strong>Tente outro período, categoria ou faixa de preço.</div>';
  }

  el.querySelector('#busca').addEventListener('submit', (ev) => {
    ev.preventDefault();
    const f = ev.target;
    const novo = { inicio: f.inicio.value, fim: f.fim.value, categoria: f.categoria.value, busca: f.busca.value.trim(), precoMin: f.precoMin.value, precoMax: f.precoMax.value };
    const erros = {};
    if (!novo.inicio || !novo.fim || new Date(novo.fim) <= new Date(novo.inicio)) erros.fim = 'A devolução deve ser posterior à retirada.';
    if (novo.precoMin && novo.precoMax && Number(novo.precoMin) > Number(novo.precoMax)) erros.precoMax = 'O preço mínimo não pode ser maior que o máximo.';
    f.querySelectorAll('[data-erro]').forEach((s) => (s.textContent = erros[s.dataset.erro] || ''));
    if (Object.keys(erros).length) return;
    filtro = novo;
    carregar(el.querySelector('#resultado'), buscar);
  });
  el.querySelector('#resultado').addEventListener('click', (e) => {
    const b = e.target.closest('[data-reservar]');
    if (b) novaReserva({ atendimento: false, idVeiculo: b.dataset.reservar, inicio: filtro.inicio, fim: filtro.fim, aoCriar: buscar });
  });
  carregar(el.querySelector('#resultado'), buscar);
}

function reservas(el) {
  let lista = [];
  const colunas = [
    { titulo: 'Veículo', celula: (r) => `<strong>${esc(r.veiculo.marca + ' ' + r.veiculo.modelo)}</strong><div class="mono suave">${esc(r.veiculo.placa)}</div>` },
    { titulo: 'Período', celula: (r) => `<div class="suave">${dataHora(r.dataInicio)}<br>até ${dataHora(r.dataFim)}</div>` },
    { titulo: 'Valor previsto', celula: (r) => `<b>${moeda(r.valorTotalPrevisto)}</b>` },
    { titulo: 'Status', celula: (r) => badge(r.status) },
    { titulo: '', celula: (r) => (r.status === 'PENDENTE' || r.status === 'CONFIRMADA' ? `<button class="btn btn-leve btn-pequeno" data-cancelar="${r.id}">Cancelar</button>` : '') },
  ];
  async function recarregar() {
    lista = await api.get('/reservas');
    el.querySelector('#tabela').innerHTML = tabela(colunas, lista, 'Você ainda não tem reservas.');
  }
  el.innerHTML = cabecalho('Minhas reservas', 'Acompanhe e gerencie suas reservas.', '<button class="btn btn-primario" id="nova">+ Nova reserva</button>') + '<section class="card"><div id="tabela"></div></section>';
  el.querySelector('#nova').addEventListener('click', () => novaReserva({ atendimento: false, aoCriar: recarregar }));
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const b = e.target.closest('[data-cancelar]');
    if (b && await confirmar('Cancelar reserva', 'Deseja cancelar esta reserva? Esta ação não pode ser desfeita.', 'Cancelar reserva')) {
      try {
        await api.patch(`/reservas/${b.dataset.cancelar}/cancelar`);
        toast('Reserva cancelada.');
        await recarregar();
      } catch (err) {
        toast(err.message, 'erro');
      }
    }
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

function locacoes(el) {
  const colunas = [
    { titulo: 'Veículo', celula: (l) => `<strong>${esc(l.veiculo.marca + ' ' + l.veiculo.modelo)}</strong><div class="mono suave">${esc(l.veiculo.placa)}</div>` },
    { titulo: 'Retirada', celula: (l) => `<span class="suave">${dataHora(l.dataRetirada)}</span>` },
    { titulo: 'Devolução', celula: (l) => `<div class="suave">Prevista: ${dataHora(l.dataPrevistaDevolucao)}${l.dataDevolucaoEfetiva ? `<br>Efetiva: ${dataHora(l.dataDevolucaoEfetiva)}` : ''}</div>` },
    { titulo: 'Total', celula: (l) => `<b>${moeda(l.valorDevido)}</b>${l.multaAcumulada || l.valorMulta > 0 ? `<div class="suave" style="color:#be123c">inclui multa ${moeda(l.multaAcumulada || l.valorMulta)}</div>` : ''}` },
    { titulo: 'Status', celula: (l) => badge(l.status) },
  ];
  el.innerHTML = cabecalho('Minhas locações', 'Histórico e locações em andamento.') + '<section class="card"><div id="tabela"></div></section>';
  carregar(el.querySelector('#tabela'), async () => {
    el.querySelector('#tabela').innerHTML = tabela(colunas, await api.get('/locacoes'), 'Nenhuma locação ainda.');
  });
}

function pagamentos(el) {
  const colunas = [
    { titulo: 'Data', celula: (p) => dataHora(p.dataPagamento) },
    { titulo: 'Locação', celula: (p) => `#${p.locacao.id}` },
    { titulo: 'Forma', celula: (p) => rotulo(p.formaPagamento) },
    { titulo: 'Valor', celula: (p) => `<b>${moeda(p.valor)}</b>` },
    { titulo: 'Status', celula: (p) => badge(p.status) },
    { titulo: 'Observação', celula: (p) => `<span class="suave">${esc(p.observacao || '—')}</span>` },
  ];
  el.innerHTML = cabecalho('Meus pagamentos', 'Histórico dos seus pagamentos.') + '<section class="card"><div id="tabela"></div></section>';
  carregar(el.querySelector('#tabela'), async () => {
    el.querySelector('#tabela').innerHTML = tabela(colunas, await api.get('/pagamentos'), 'Nenhum pagamento registrado.');
  });
}

function perfil(el) {
  el.innerHTML = cabecalho('Meus dados', 'Mantenha seu cadastro e sua CNH atualizados.') + '<section class="card card-corpo" id="dados" style="max-width:680px"></section>';
  carregar(el.querySelector('#dados'), async () => {
    const c = await api.get('/clientes/me');
    const area = el.querySelector('#dados');
    area.innerHTML = `<form class="form-grade" novalidate>${formularioCliente(c, { alteraCpf: false })}
      <p class="inteiro">Situação da CNH: ${badge(c.cnhSituacao)}</p>
      <div class="form-acoes"><button class="btn btn-primario" type="submit">Salvar alterações</button></div></form>`;
    aoEnviar(area.querySelector('form'), (x) => validarCliente(x, { alteraCpf: false }), async (x) => {
      await api.put('/clientes/me', corpoCliente({ ...x, cpf: c.cpf }));
      toast('Dados atualizados.');
      perfil(el);
    });
  });
}

import { api, dataHora, esc, moeda, rotulo } from '../api.js';
import { abrirModal, aoEnviar, badge, cabecalho, campo, carregar, confirmar, filtrarTexto, opcoes, selecao, tabela, toast } from '../ui.js';

const FORMAS = opcoes(['PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'DINHEIRO']);
const STATUS = opcoes(['PAGO', 'PARCIAL', 'ESTORNADO']);

export function render(el, usuario) {
  const admin = usuario.perfil === 'ADMIN';
  let lista = [];

  const colunas = [
    { titulo: 'Data', celula: (p) => dataHora(p.dataPagamento) },
    { titulo: 'Locação', celula: (p) => `#${p.locacao.id}` },
    { titulo: 'Cliente', celula: (p) => `<strong>${esc(p.locacao.cliente.nomeCompleto)}</strong>` },
    { titulo: 'Forma', celula: (p) => rotulo(p.formaPagamento) },
    { titulo: 'Valor', celula: (p) => `<b>${moeda(p.valor)}</b>` },
    { titulo: 'Status', celula: (p) => badge(p.status) },
    { titulo: 'Obs.', celula: (p) => `<span class="suave">${esc(p.observacao || '—')}</span>` },
    { titulo: 'Ações', celula: (p) => (admin && (p.status === 'PAGO' || p.status === 'PARCIAL') ? `<div class="acoes"><button class="btn btn-leve btn-pequeno" data-estornar="${p.id}">Estornar</button></div>` : '') },
  ];

  function desenhar() {
    let itens = filtrarTexto(lista, el.querySelector('#f-busca').value, (p) => `${p.locacao.cliente.nomeCompleto} ${p.locacao.id}`);
    const st = el.querySelector('#f-status').value;
    if (st) itens = itens.filter((p) => p.status === st);
    el.querySelector('#tabela').innerHTML = tabela(colunas, itens, 'Nenhum pagamento encontrado.');
  }

  async function recarregar() {
    lista = await api.get('/pagamentos');
    desenhar();
  }

  async function novo() {
    const locacoes = (await api.get('/locacoes'));
    const m = abrirModal('Registrar pagamento', `<form class="form-grade" novalidate>
      ${selecao('Locação', 'idLocacao', locacoes.map((l) => ({ valor: l.id, rotulo: `#${l.id} · ${l.cliente.nomeCompleto} · ${l.veiculo.modelo}` })), { classe: 'inteiro' })}
      <div id="saldo" class="aviso inteiro" hidden aria-live="polite"></div>
      ${campo('Valor (R$)', 'valor', { tipo: 'number', extra: 'min="0" step="0.01"' })}
      ${selecao('Forma de pagamento', 'formaPagamento', FORMAS, { vazio: null })}
      ${campo('Observação', 'observacao', { obrigatorio: false, classe: 'inteiro', extra: 'maxlength="255"' })}
      <p class="dica inteiro">O status (pago ou parcial) é definido automaticamente pelo saldo da locação.</p>
      <div class="form-acoes"><button type="button" class="btn btn-sec" data-cancelar>Cancelar</button><button type="submit" class="btn btn-primario">Registrar</button></div></form>`);
    const form = m.el.querySelector('form');
    form.querySelector('[data-cancelar]').addEventListener('click', m.fechar);
    form.elements.idLocacao.addEventListener('change', async () => {
      const caixa = m.el.querySelector('#saldo');
      if (!form.elements.idLocacao.value) {
        caixa.hidden = true;
        return;
      }
      try {
        const s = await api.get(`/pagamentos/locacao/${form.elements.idLocacao.value}/saldo`);
        caixa.hidden = false;
        caixa.innerHTML = `Devido ${moeda(s.valorDevido)} · Pago ${moeda(s.valorPago)} · <b>Saldo ${moeda(s.saldo)}</b>`;
        if (!form.elements.valor.value && s.saldo > 0) form.elements.valor.value = s.saldo;
      } catch (e) {
        caixa.hidden = true;
      }
    });
    aoEnviar(form, (x) => {
      const e = {};
      if (!x.idLocacao) e.idLocacao = 'Selecione a locação.';
      if (!(Number(x.valor) > 0)) e.valor = 'Informe um valor maior que zero.';
      return e;
    }, async (x) => {
      const p = await api.post('/pagamentos', { idLocacao: Number(x.idLocacao), valor: Number(x.valor), formaPagamento: x.formaPagamento, observacao: x.observacao || null });
      toast(`Pagamento registrado (${rotulo(p.status).toLowerCase()}).`);
      m.fechar();
      await recarregar();
    });
  }

  el.innerHTML = cabecalho('Pagamentos', 'Histórico de pagamentos (simulados).', '<button class="btn btn-primario" id="novo">+ Registrar pagamento</button>') + `
    <section class="card"><div class="filtros"><input id="f-busca" placeholder="Buscar por cliente ou nº da locação" aria-label="Buscar pagamento">
    <select id="f-status" aria-label="Status"><option value="">Todos os status</option>${STATUS.map((s) => `<option value="${s.valor}">${s.rotulo}</option>`).join('')}</select></div><div id="tabela"></div></section>`;
  el.querySelectorAll('.filtros input, .filtros select').forEach((c) => c.addEventListener('input', desenhar));
  el.querySelector('#novo').addEventListener('click', () => novo().catch((e) => toast(e.message, 'erro')));
  el.querySelector('#tabela').addEventListener('click', async (e) => {
    const b = e.target.closest('[data-estornar]');
    if (!b) return;
    const p = lista.find((x) => x.id === Number(b.dataset.estornar));
    if (await confirmar('Estornar pagamento', `Deseja estornar o pagamento de ${moeda(p.valor)} da locação #${p.locacao.id}?`, 'Estornar')) {
      try {
        await api.patch(`/pagamentos/${p.id}/estorno`);
        toast('Pagamento estornado.');
        await recarregar();
      } catch (err) {
        toast(err.message, 'erro');
      }
    }
  });
  carregar(el.querySelector('#tabela'), recarregar);
}

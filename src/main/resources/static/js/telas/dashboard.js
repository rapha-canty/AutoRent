import { api, dataHora, esc, moeda, rotulo } from '../api.js';
import { badge, carregar } from '../ui.js';

const CORES_FROTA = { DISPONIVEL: '#10b981', RESERVADO: '#f59e0b', ALUGADO: '#3b5bff', MANUTENCAO: '#f97316', INATIVO: '#94a3b8' };

const nomeMes = (m) => {
  const [a, mes] = m.split('-');
  return new Date(Number(a), Number(mes) - 1, 1).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '');
};

export function render(el, usuario) {
  carregar(el, async () => {
    const d = await api.get('/dashboard');
    const disponiveis = d.frota.DISPONIVEL;
    const maxMes = Math.max(1, ...Object.values(d.locacoesPorMes));
    const totalFrota = Math.max(1, Object.values(d.frota).reduce((a, b) => a + b, 0));

    el.innerHTML = `
      <div class="banner">
        <p>${usuario.perfil === 'ADMIN' ? 'Visão geral' : 'Painel operacional'}</p>
        <h1>Bem-vindo(a), ${esc(usuario.nome.split(' ')[0])}!</h1>
        <p>${d.locacoesAtrasadas > 0 ? `Atenção: ${d.locacoesAtrasadas} devolução(ões) em atraso.` : 'Nenhuma devolução em atraso. Tudo em dia na operação.'}</p>
      </div>
      <div class="stats">
        <div class="stat g1"><small>Veículos cadastrados</small><strong>${d.totalVeiculos}</strong></div>
        <div class="stat g2"><small>Disponíveis</small><strong>${disponiveis}</strong></div>
        <div class="stat g3"><small>Alugados</small><strong>${d.frota.ALUGADO}</strong></div>
        <div class="stat g4"><small>Reservas pendentes</small><strong>${d.reservasPendentes}</strong></div>
        <div class="stat g5"><small>Locações em andamento</small><strong>${d.locacoesEmAndamento}</strong></div>
        ${d.receitaMes !== null ? `<div class="stat g6"><small>Receita do mês</small><strong>${moeda(d.receitaMes)}</strong></div>` : ''}
      </div>
      <div class="grade-2">
        <section class="card"><div class="card-cab"><h2>Locações por mês</h2></div>
          <div class="card-corpo"><div class="barras">${Object.entries(d.locacoesPorMes).map(([mes, q]) =>
            `<div><span>${q}</span><i style="height:${Math.round((q / maxMes) * 150)}px"></i><span>${nomeMes(mes)}</span></div>`).join('')}</div></div></section>
        <section class="card"><div class="card-cab"><h2>Distribuição da frota</h2></div>
          <div class="card-corpo">${Object.entries(d.frota).map(([s, q]) =>
            `<div class="frota-linha"><span class="rotulo">${rotulo(s)}</span><span class="trilho"><i style="width:${(q / totalFrota) * 100}%;background:${CORES_FROTA[s]}"></i></span><b>${q}</b></div>`).join('')}</div></section>
      </div>
      <div class="grade-2">
        <section class="card"><div class="card-cab"><h2>Reservas recentes</h2><a href="#/reservas">Ver todas</a></div>
          ${d.reservasRecentes.length ? d.reservasRecentes.map((r) => `<div class="card-corpo" style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;border-bottom:1px solid #f8fafc">
            <div><strong>${esc(r.cliente.nomeCompleto)}</strong><div class="suave">${esc(r.veiculo.marca + ' ' + r.veiculo.modelo)} · ${dataHora(r.dataInicio)}</div></div>
            <div><b>${moeda(r.valorTotalPrevisto)}</b> ${badge(r.status)}</div></div>`).join('') : '<div class="vazio"><strong>Nenhuma reserva ainda</strong></div>'}</section>
        <div style="display:grid;gap:20px;align-content:start">
          <section class="card"><div class="card-cab"><h2>Devoluções previstas hoje</h2></div>
            ${d.devolucoesHoje.length ? d.devolucoesHoje.map((l) => `<div class="card-corpo" style="border-bottom:1px solid #f8fafc"><strong>${esc(l.veiculo.modelo)}</strong>
              <div class="suave">${esc(l.cliente.nomeCompleto)} · ${dataHora(l.dataPrevistaDevolucao)} ${l.status === 'ATRASADA' ? badge('ATRASADA') : ''}</div></div>`).join('') : '<div class="vazio"><strong>Nenhuma devolução hoje</strong></div>'}</section>
          <section class="card"><div class="card-cab"><h2>Ações rápidas</h2></div>
            <div class="card-corpo" style="display:grid;gap:8px"><a class="btn btn-sec" href="#/reservas">Nova reserva</a><a class="btn btn-sec" href="#/clientes">Novo cliente</a><a class="btn btn-sec" href="#/locacoes">Retirada / devolução</a></div></section>
        </div>
      </div>`;
  });
}

const CHAVE = 'autorent.auth';

export const sessao = {
  obter: () => sessionStorage.getItem(CHAVE),
  salvar: (valor) => sessionStorage.setItem(CHAVE, valor),
  limpar: () => sessionStorage.removeItem(CHAVE),
};

export class ErroApi extends Error {
  constructor(status, mensagem, campos) {
    super(mensagem);
    this.status = status;
    this.campos = campos || {};
  }
}

async function chamar(metodo, url, corpo) {
  const cabecalhos = {};
  const auth = sessao.obter();
  if (auth) cabecalhos.Authorization = 'Basic ' + auth;
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';

  let resposta;
  try {
    resposta = await fetch('/api' + url, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi(0, 'Não foi possível conectar ao servidor. Verifique se o sistema está em execução.');
  }

  if (resposta.status === 204) return null;
  const texto = await resposta.text();
  let dados = null;
  try {
    dados = texto ? JSON.parse(texto) : null;
  } catch {
    dados = null;
  }

  if (!resposta.ok) {
    if (resposta.status === 401 && auth && url !== '/eu') document.dispatchEvent(new Event('sessao-expirada'));
    throw new ErroApi(resposta.status, (dados && dados.mensagem) || 'Erro inesperado ao processar a solicitação.', dados && dados.campos);
  }
  return dados;
}

export const api = {
  get: (url) => chamar('GET', url),
  post: (url, corpo) => chamar('POST', url, corpo === undefined ? {} : corpo),
  put: (url, corpo) => chamar('PUT', url, corpo),
  patch: (url) => chamar('PATCH', url),
  del: (url) => chamar('DELETE', url),
};

export async function entrar(email, senha) {
  sessao.salvar(btoa(unescape(encodeURIComponent(email.trim() + ':' + senha))));
  try {
    return await api.get('/eu');
  } catch (e) {
    sessao.limpar();
    throw e;
  }
}

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const moeda = (v) => (v === null || v === undefined ? '—' : brl.format(v));
export const dataHora = (iso) => (iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '—');
export const data = (iso) => (iso ? new Date(iso.length === 10 ? iso + 'T00:00:00' : iso).toLocaleDateString('pt-BR') : '—');
export const cpfMask = (c) => (c && c.length === 11 ? c.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4') : c || '');
export const soDigitos = (v) => (v || '').replace(/\D/g, '');

export const esc = (v) =>
  String(v === null || v === undefined ? '' : v).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

export function paraInputDataHora(d) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

export const paraApiDataHora = (v) => (v && v.length === 16 ? v + ':00' : v);

const ROTULOS = {
  ECONOMICO: 'Econômico', COMPACTO: 'Compacto', SEDAN: 'Sedã', SUV: 'SUV', PICAPE: 'Picape', LUXO: 'Luxo',
  DISPONIVEL: 'Disponível', RESERVADO: 'Reservado', ALUGADO: 'Alugado', MANUTENCAO: 'Manutenção', INATIVO: 'Inativo',
  PENDENTE: 'Pendente', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', CONCLUIDA: 'Concluída',
  ATIVA: 'Ativa', ATRASADA: 'Atrasada', FINALIZADA: 'Finalizada',
  PAGO: 'Pago', PARCIAL: 'Parcial', ESTORNADO: 'Estornado',
  PIX: 'Pix', CARTAO_CREDITO: 'Cartão de crédito', CARTAO_DEBITO: 'Cartão de débito', DINHEIRO: 'Dinheiro',
  ADMIN: 'Administrador', VENDEDOR: 'Vendedor', CLIENTE: 'Cliente',
  VALIDA: 'Válida', VENCIDA: 'Vencida', INEXISTENTE: 'Inexistente', IRREGULAR: 'Irregular', NAO_VERIFICADA: 'Não verificada',
};

export const rotulo = (valor) => ROTULOS[valor] || valor;

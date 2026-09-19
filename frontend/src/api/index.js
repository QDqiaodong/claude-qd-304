import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(new Error(err?.response?.data?.message || err.message || '请求没成功'))
)

export default {
  cabinets: {
    list: (params) => http.get('/cabinets', { params }),
    add: (body) => http.post('/cabinets', body),
    save: (id, body) => http.put(`/cabinets/${id}`, body)
  },
  reagents: {
    list: (params) => http.get('/reagents', { params }),
    expiring: (days) => http.get('/reagents/expiring', { params: { days } }),
    add: (body) => http.post('/reagents', body),
    save: (id, body) => http.put(`/reagents/${id}`, body)
  },
  instruments: {
    list: (params) => http.get('/instruments', { params }),
    add: (body) => http.post('/instruments', body),
    save: (id, body) => http.put(`/instruments/${id}`, body)
  },
  usages: {
    list: (params) => http.get('/usages', { params }),
    add: (body) => http.post('/usages', body)
  }
}

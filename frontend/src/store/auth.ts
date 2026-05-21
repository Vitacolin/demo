import { defineStore } from 'pinia'
import axios from 'axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: null as any
  }),
  getters: {
    isAuthenticated: (state) => !!state.token
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    clearAuth() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
    },
    async fetchUser() {
      if (!this.token) return
      try {
        const res = await axios.get('http://localhost:8000/api/auth/me', {
          headers: { Authorization: `Bearer ${this.token}` }
        })
        this.user = res.data
      } catch (error) {
        this.clearAuth()
      }
    }
  }
})

import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: null as any,
    isAdmin: false
  }),
  getters: {
    isAuthenticated: (state) => !!state.token
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setIsAdmin(isAdmin: boolean) {
      this.isAdmin = isAdmin
    },
    clearAuth() {
      this.token = null
      this.user = null
      this.isAdmin = false
      localStorage.removeItem('token')
      localStorage.removeItem('loginRole')
    },
    async fetchUser() {
      if (!this.token) return
      try {
        const res = await api.get('/auth/me')
        this.user = res.data

        const userInfo = await api.get('/system/me')

        const savedRole = localStorage.getItem('loginRole')
        if (savedRole === 'admin') {
          this.isAdmin = userInfo.data.isAdmin
        } else if (savedRole === 'user') {
          this.isAdmin = false
        } else {
          this.isAdmin = userInfo.data.isAdmin
        }
      } catch (error) {
        this.clearAuth()
      }
    }
  }
})

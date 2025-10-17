<script>
    import { API_BASE } from '../config.js';
    
    export let onLogin;
    export let onBack;
    
    let email = '';
    let password = '';
    let error = '';
    
    async function login() {
      try {
        const res = await fetch(`${API_BASE}/users/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });
  
          if (res.ok) {
          const user = await res.json();
          if (onLogin) onLogin(user);
        } else {
          error = 'Invalid credentials';
        }
      } catch (err) {
        error = `Network error: ${err.message}`;
      }
    }
  </script>
    
  <section class="card">
    <h3>Login</h3>
    <input type="email" bind:value={email} placeholder="Email" />
    <input type="password" bind:value={password} placeholder="Password" />
    <nav>
      <button on:click={login}>Login</button>
      <button on:click={onBack}>Back</button>
    </nav>
    
    {#if error}
      <p class="error">{error}</p>
    {/if}
  </section>
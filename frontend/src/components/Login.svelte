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
          error = 'Invalid credentials. Please try again.';
        }
      } catch (err) {
        error = `Network error: ${err.message}`;
      }
    }
</script>
    
<section class="card">
  <h2 style="text-align: center;">Login</h2>
  <div class="form-group">
    <h3>Email</h3>
    <input type="email" bind:value={email} placeholder="Email" />
  </div>
  <div class="form-group">
    <h3>Password</h3>
    <input type="password" bind:value={password} placeholder="Password" />
  </div>

  <div style="display: flex; flex-direction: column; gap: var(--spacing-md);">
    <button on:click={login} class="button button-primary">Login</button>
    <button on:click={onBack} class="button button-secondary">Back</button>
  </div>
  
  {#if error}
    <p class="error-message" style="text-align: center; margin-top: var(--spacing-lg);">{error}</p>
  {/if}
</section>
<script>
    export let onDone;
    import { API_BASE } from '../config.js';
    
    let username = '';
    let email = '';
    let password = '';
    
    let errors = {
      username: '',
      email: '',
      password: '',
      general: ''
    };
    
    async function createUser() {
      errors = { username: '', email: '', password: '', general: '' };
    
      if (!username.trim()) errors.username = 'Username is required';
      if (!email.trim()) errors.email = 'Email is required';
      if (!password.trim()) errors.password = 'Password is required';
      if (errors.username || errors.email || errors.password) return;
    
      const user = { username, email, password };
    
      try {
        const res = await fetch(`${API_BASE}/users`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(user)
        });
    
        if (res.ok) {
          if (onDone) onDone();
        } else if (res.status === 409) {
          errors.general = 'Username or email already exists!';
        } else {
          errors.general = `Unexpected error: ${res.statusText}`;
        }
      } catch (err) {
        errors.general = `Network error: ${err.message}`;
      }
    }
</script>
    
<section class="card">
  <div class="form-group">
    <h3>Username</h3>
    <input type="text" bind:value={username} placeholder="Username" />
    {#if errors.username}<p class="error-message">{errors.username}</p>{/if}
  </div>

  <div class="form-group">
    <h3>Email</h3>
    <input type="email" bind:value={email} placeholder="Email" />
    {#if errors.email}<p class="error-message">{errors.email}</p>{/if}
  </div>

  <div class="form-group">
    <h3>Password</h3>
    <input type="password" bind:value={password} placeholder="Password" />
    {#if errors.password}<p class="error-message">{errors.password}</p>{/if}
  </div>

  <button on:click={createUser} class="button button-primary button-full-width">Create User</button>
  
  {#if errors.general}
    <p class="error-message" style="text-align: center; margin-top: var(--spacing-lg);">{errors.general}</p>
  {/if}
</section>
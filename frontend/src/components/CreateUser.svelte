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
        } else if (res.status === 400) {
          const errorText = await res.text();
          const msgs = errorText.split(';').map(s => s.trim());
          msgs.forEach(msg => {
            if (msg.toLowerCase().includes('username')) errors.username = msg;
            else if (msg.toLowerCase().includes('email')) errors.email = msg;
            else if (msg.toLowerCase().includes('password')) errors.password = msg;
            else errors.general = msg;
          });
        } else if (res.status === 409) {
          errors.general = 'Username or email already exists!';
        } else {
          errors.general = `Unexpected error: ${res.statusText}`;
        }
      } catch (err) {
        console.error(err);
        errors.general = `Network error: ${err.message}`;
      }
    }
  </script>
    
    <section class="card">
      <h3>Username</h3>
      <input type="text" bind:value={username} placeholder="Enter username" />
      {#if errors.username}<p class="error">{errors.username}</p>{/if}
    
      <h3>Email</h3>
      <input type="email" bind:value={email} placeholder="Enter email" />
      {#if errors.email}<p class="error">{errors.email}</p>{/if}
    
      <h3>Password</h3>
      <input type="password" bind:value={password} placeholder="Enter password" />
      {#if errors.password}<p class="error">{errors.password}</p>{/if}
    
      <nav>
        <button on:click={createUser}>Create user</button>
      </nav>
    
      {#if errors.general}
        <p class="general">{errors.general}</p>
      {/if}
    </section>
    
    <style>
      .error { color: #ff5555; font-size: 0.9rem; margin-top: 0.25rem; }
      .general { color: #ffcc00; margin-top: 1rem; }
    </style>
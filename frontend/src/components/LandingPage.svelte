<script>
    export let onLogin;
    export let onRegister;
    export let onGuest;
</script>
    
<section class="card">
  <h2 style="text-align:center; font-size: 1.8rem;">Welcome</h2>
  <div style="display: flex; flex-direction: column; gap: var(--spacing-md); margin-top: var(--spacing-lg);">
    <button on:click={onLogin} class="button button-primary">Log In</button>
    <button on:click={onRegister} class="button button-success">Create New User</button>
    <button on:click={onGuest} class="button button-secondary">Continue as Guest</button>
  </div>
</section>
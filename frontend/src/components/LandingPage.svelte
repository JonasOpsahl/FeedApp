<script>
    export let onLogin;
    export let onRegister;
    export let onGuest;
  </script>
    
  <section class="card">
    <h2>Welcome</h2>
    <nav>
      <button on:click={onLogin}>Log in</button>
      <button on:click={onRegister}>Create new user</button>
      <button on:click={onGuest}>Continue as guest</button>
    </nav>
  </section>
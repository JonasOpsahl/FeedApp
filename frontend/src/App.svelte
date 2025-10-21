<script>
  import LandingPage from './components/LandingPage.svelte';
  import CreateUser from './components/CreateUser.svelte';
  import CreatePoll from './components/CreatePoll.svelte';
  import Vote from './components/Vote.svelte';
  import Login from './components/Login.svelte';
  import AdminPolls from './components/AdminPolls.svelte';

  import './app.css';
  
  export let currentUser = null;

  let currentView = 'landing';
  let mainTab = 'vote';
  let isGuest = false;

  function goToLanding() { 
    currentView = 'landing'; 
    currentUser = null;
    isGuest = false;
  }
  
  function goToMain(user) { 
    currentUser = user; 
    isGuest = false;
    currentView = 'main'; 
  }

  function goToGuest() {
    currentUser = null;
    isGuest = true;
    currentView = 'main';
  }

  function showRegister() { currentView = 'register'; }
</script>

<div id="app">
  <header>
    <h1>Poll Application</h1>
  </header>

  <main>
    {#if currentView === 'landing'}
      <LandingPage 
        onLogin={() => currentView = 'login'} 
        onRegister={showRegister} 
        onGuest={goToGuest} />
  
    {:else if currentView === 'register'}
      <CreateUser onDone={() => currentView = 'login'} />
      <div style="text-align: center; margin-top: var(--spacing-lg);">
          <button class="button button-secondary" on:click={goToLanding}>Back to Welcome</button>
      </div>
  
    {:else if currentView === 'login'}
      <Login onLogin={goToMain} onBack={goToLanding} />
  
    {:else if currentView === 'main'}
      {#if isGuest}
        <Vote />
  
      {:else if currentUser}
        <div class="tab-nav">
          <button on:click={() => mainTab = 'createPoll'} class="tab-button" class:active={mainTab === 'createPoll'}>
            Create Poll
          </button>
          <button on:click={() => mainTab = 'vote'} class="tab-button" class:active={mainTab === 'vote'}>
            Vote
          </button>
          <button on:click={() => mainTab = 'admin'} class="tab-button" class:active={mainTab === 'admin'}>
            Manage Polls
          </button>
        </div>
  
        {#if mainTab === 'createPoll'}
          <CreatePoll 
            onDone={() => mainTab = 'vote'} 
            currentUser={currentUser} 
          />

        {:else if mainTab === 'vote'}
          <Vote currentUser={currentUser} />

        {:else if mainTab === 'admin'}
          <AdminPolls currentUser={currentUser} />
        {/if}
      {/if}
    {/if}
  </main>

  <footer>
    {#if isGuest}
      <div>
        You are in guest mode.
        <button class="button button-secondary" on:click={goToLanding}>Back to Login</button>
      </div>
    {:else if currentUser}
      <div>
        Logged in as <strong>{currentUser.username}</strong>
        <button class="button button-danger" on:click={goToLanding}>Log Out</button>
      </div>
    {/if}
  </footer>
</div>
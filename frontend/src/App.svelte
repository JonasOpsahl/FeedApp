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
      <nav>
        <button on:click={goToLanding}>Back</button>
      </nav>
  
    {:else if currentView === 'login'}
      <Login onLogin={goToMain} onBack={goToLanding} />
  
    {:else if currentView === 'main'}
      {#if isGuest}
        <Vote />
  
      {:else if currentUser}
        <nav>
          <button on:click={() => mainTab = 'createPoll'} class:active={mainTab === 'createPoll'}>
            Create poll
          </button>
          <button on:click={() => mainTab = 'vote'} class:active={mainTab === 'vote'}>
            Vote on polls
          </button>
          <button on:click={() => mainTab = 'admin'} class:active={mainTab === 'admin'}>
            Administer polls
          </button>
        </nav>
  
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

  <footer style="margin-top:2rem; text-align:center;">
    {#if isGuest}
      <div>
        You are in guest mode
        <nav>
          <button on:click={goToLanding} style="margin-left:1rem;">Back to login</button>
        </nav>
      </div>
    {:else if currentUser}
      <div>
        Logged in as <strong>{currentUser.username}  </strong>
        <nav>
          <button on:click={goToLanding} style="margin-top:1rem;">Log out</button>
        </nav>
      </div>
    {/if}
  </footer>
</div>

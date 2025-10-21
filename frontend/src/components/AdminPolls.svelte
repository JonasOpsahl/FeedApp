<script>
    import { onMount } from 'svelte';
    import { API_BASE } from '../config.js';
    export let currentUser;
  
    let userPolls = [];
    let pollToDelete = null;
  
    onMount(async () => {
      if (!currentUser) return;
  
      try {
        const res = await fetch(`${API_BASE}/polls`);
        if (res.ok) {
          const data = await res.json();
          userPolls = data.filter(poll => poll.createdByUser?.id === currentUser.id);
        } else {
          console.error('Failed to load polls', await res.text());
        }
      } catch (err) {
        console.error('Network error', err);
      }
    });
  
    function confirmDelete(poll) {
      pollToDelete = poll;
    }
  
    function cancelDelete() {
      pollToDelete = null;
    }
  
    async function deletePoll(pollId) {
      try {
        const res = await fetch(`${API_BASE}/polls/${pollId}/${currentUser.id}`, {
          method: "DELETE"
        });
        if (res.ok) {
          userPolls = userPolls.filter(p => p.id !== pollId);
          pollToDelete = null;
        } else {
          console.error('Failed to delete poll', await res.text());
        }
      } catch (err) {
        console.error('Network error', err);
      }
    }
</script>
  
<div class="card">
  {#if userPolls.length === 0}
    <p>You have not created any polls yet.</p>
  {:else}
    <h3>Your Polls</h3>
    <ul>
      {#each userPolls as poll (poll.id)}
        <li class="poll-item">
          <span>{poll.question}</span>
          <button on:click={() => confirmDelete(poll)} class="button button-danger button-sm">Delete</button>
        </li>
      {/each}
    </ul>
  {/if}

  {#if pollToDelete}
    <div class="overlay">
      <div class="confirm-box">
        <p>Are you sure you want to delete the poll "{pollToDelete.question}"?</p>
        <div class="buttons">
          <button on:click={cancelDelete} class="button button-secondary">Cancel</button>
          <button on:click={() => deletePoll(pollToDelete.id)} class="button button-danger">Delete</button>
        </div>
      </div>
    </div>
  {/if}
</div>
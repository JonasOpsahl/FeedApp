<script lang="ts">
    import { onMount } from 'svelte';
    import { API_BASE } from '../config.js';
  
    export let currentUser = null;
  
    let polls = [];
    let selectedPollId: number = null;
    let selectedPoll = null;
    let selectedOption = '';
    let voteMessage = '';
    let loading = true;
  
    onMount(async () => {
      try {
        const res = await fetch(`${API_BASE}/polls`);
        if (res.ok) {
          let data = await res.json();
          let userId = currentUser ? Number(currentUser.id) : null;
  
          polls = data.filter(poll => {
            if (poll.public) return true;
            if (userId != null && poll.invitedUserIds.includes(userId)) return true;
            return false;
          });
  
          if (polls.length > 0) {
            selectedPollId = polls[0].id;
            await fetchSelectedPoll(selectedPollId);
          }
        }
      } catch (err) {
        console.error("Error fetching polls", err);
      } finally {
        loading = false;
      }
    });
  
    async function fetchSelectedPoll(pollId: number) {
      const pollRes = await fetch(`${API_BASE}/polls/${pollId}`);
      const pollData = await pollRes.json();
  
      const votesRes = await fetch(`${API_BASE}/polls/${pollId}/votes`);
      const voteCounts = await votesRes.json();
  
      selectedPoll = {
          ...pollData,
          voteCounts,
          totalVotes: voteCounts.reduce((sum, v) => sum + v.voteCount, 0)
      };
    }
  
    async function submitVote(option) {
      if (!currentUser) {
          alert('You must be logged in to vote!');
          return;
      }
  
      try {
          const res = await fetch(`${API_BASE}/polls/${selectedPoll.id}/votes`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                  userId: currentUser.id,
                  voteOptionId: option.id
              })
          });
  
          if (!res.ok) {
              const data = await res.json();
              voteMessage = `Vote not accepted: ${data.error || data.message}`;
              return;
          }
  
          await fetchSelectedPoll(selectedPoll.id);
          selectedOption = option.caption;
          voteMessage = `You voted for: ${option.caption}`;
      } catch (err) {
          console.error('Network error while voting', err);
          voteMessage = 'Network error while voting';
      }
    }
  
    function votePercentage(optionVoteCount: number) {
      const total = selectedPoll?.totalVotes || 0;
      if (total === 0) return 0;
      return Math.round((optionVoteCount / total) * 100);
    }
  </script>
  
  <div class="card">
    <h3>Available polls:</h3>
  
    {#if loading}
      <p>Loading polls...</p>
    {:else if polls.length === 0}
      <p>No polls available for you.</p>
    {:else}
      <select bind:value={selectedPollId} on:change={async () => {
        if (selectedPollId) {
          await fetchSelectedPoll(selectedPollId);
          selectedOption = '';
          voteMessage = '';
        }
      }}>
        {#each polls as poll (poll.id)}
          <option value={poll.id}>{poll.question}</option>
        {/each}
      </select>
  
      {#if selectedPoll && selectedPoll.options.length > 0}
        <h3 style="margin-top: 1.5rem;">Options:</h3>
        <div class="options">
          {#each selectedPoll.options as option}
            <button on:click={() => submitVote(option)}>
              {option.caption} - {selectedPoll.voteCounts.find(v => v.optionCaption === option.caption)?.voteCount || 0} votes
              ({votePercentage(selectedPoll.voteCounts.find(v => v.optionCaption === option.caption)?.voteCount || 0)}%)
            </button>
          {/each}
        </div>
      {/if}
  
      {#if voteMessage}
        <p style="margin-top: 2rem; color: #ffcc00;">
          {voteMessage}
        </p>
      {/if}
    {/if}
  </div>
  
  <style>
    .options {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin-top: 1rem;
    }
  
    .options button {
      width: 100%;
      text-align: left;
      padding: 0.6rem 1rem;
      border-radius: 6px;
      background-color: #1a1a1a;
      border: 1px solid transparent;
      color: #fff;
      cursor: pointer;
      transition: border-color 0.25s, background-color 0.25s;
    }
  
    .options button:hover {
      border-color: #646cff;
      background-color: #1a1aff20;
    }
  </style>
<script lang="ts">
    import { onMount, tick } from 'svelte';
    import { API_BASE } from '../config.js';
  
    export let currentUser = null;
  
    let polls = [];
    let selectedPollId: number = null;
    let selectedPoll = null;
    let voteMessage = '';
    let loading = true;
    let hasVoted = false;
  
    onMount(async () => {
      await loadPolls();
    });

    async function loadPolls() {
      loading = true;
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
    }
  
    async function fetchSelectedPoll(pollId: number) {
      hasVoted = false;
      voteMessage = '';
      const pollRes = await fetch(`${API_BASE}/polls/${pollId}`);
      const pollData = await pollRes.json();
  
      const votesRes = await fetch(`${API_BASE}/polls/${pollId}/votes`);
      const voteCounts = await votesRes.json();
  
      selectedPoll = {
          ...pollData,
          voteCounts,
          totalVotes: voteCounts.reduce((sum, v) => sum + v.voteCount, 0)
      };
      
      if (currentUser) {
        const userVotesRes = await fetch(`${API_BASE}/polls/${pollId}/votes/${currentUser.id}`);
        if (userVotesRes.ok) {
            hasVoted = true;
        }
      }
    }
  
    async function submitVote(option) {
      if (!currentUser) {
          alert('You must be logged in to vote!');
          return;
      }
      if (hasVoted) return;

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
  
          voteMessage = `You voted for: ${option.caption}`;
          hasVoted = true;
          await tick();
          await fetchSelectedPoll(selectedPoll.id);
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
  <h3>Available polls</h3>

  {#if loading}
    <p>Loading polls...</p>
  {:else if polls.length === 0}
    <p>No polls available for you to vote on right now.</p>
  {:else}
    <select bind:value={selectedPollId} on:change={async () => {
      if (selectedPollId) await fetchSelectedPoll(selectedPollId);
    }}>
      {#each polls as poll (poll.id)}
        <option value={poll.id}>{poll.question}</option>
      {/each}
    </select>

    {#if selectedPoll}
        {#if hasVoted}
            <div style="margin-top: var(--spacing-lg);">
                <h3>Results</h3>
                {#each selectedPoll.options as option}
                    {@const voteCount = selectedPoll.voteCounts.find(v => v.optionCaption === option.caption)?.voteCount || 0}
                    <div style="margin-bottom: var(--spacing-md);">
                        <div class="vote-result-label">
                            <span>{option.caption}</span>
                            <strong>{voteCount} votes ({votePercentage(voteCount)}%)</strong>
                        </div>
                        <div class="progress-bar-container">
                            <div class="progress-bar" style="width: {votePercentage(voteCount)}%;"></div>
                        </div>
                    </div>
                {/each}
            </div>
        {:else}
            <div style="margin-top: var(--spacing-lg);">
                <h3>Choose an option</h3>
                {#each selectedPoll.options as option}
                    <button class="vote-option" on:click={() => submitVote(option)}>
                        {option.caption}
                    </button>
                {/each}
            </div>
        {/if}

        {#if voteMessage}
            <p class="success-message">{voteMessage}</p>
        {/if}
    {/if}
  {/if}
</div>
import type { Match } from "../types/Match";

export function HomePage({ matches }: { matches: Match[] }) {
  return (
    <section className="view active">
      <div className="section-head">
        <div>
          <p className="eyebrow">Home / Live dashboard</p>
          <h2>Today&apos;s Matches</h2>
        </div>
      </div>
      <div className="grid">
        {matches.map(match => (
          <article className="card" key={match.id}>
            <p className="status">{match.status}</p>
            <h3>{match.homeTeam.name} {match.homeScore}-{match.awayScore} {match.awayTeam.name}</h3>
            <p>{match.homeTeam.league} · {match.venue}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

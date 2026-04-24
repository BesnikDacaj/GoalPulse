import type { Team } from "./Team";
import type { Player } from "./Player";

export type MatchStatus = "scheduled" | "live" | "finished";

export interface MatchEvent {
  id: number;
  matchId: number;
  playerId: number;
  player: string;
  eventType: string;
  minute: number;
  detail: string;
}

export interface Match {
  id: number;
  homeTeam: Team;
  awayTeam: Team;
  date: string;
  status: MatchStatus;
  homeScore: number;
  awayScore: number;
  venue: string;
  referee: string;
  events?: MatchEvent[];
  lineups?: Player[];
}

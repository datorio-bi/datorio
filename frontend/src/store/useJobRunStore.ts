import { create } from 'zustand'
import { JobStatusResponse } from '@/api/controls'

/** The run started by a RUN_JOB button: kept outside the button, so a widget that is unmounted (a tab change,
 *  a layout change) and mounted again shows the same run and keeps asking for its state. */
export interface JobProgress {
  runId: string
  startedAt: number
  status?: JobStatusResponse
  pollErrors: number
  /** The end of the run was already announced (toast, report refresh) */
  notified?: boolean
}

interface JobRunState {
  /** By report id and widget id */
  jobs: Record<string, JobProgress | null>
  setJob: (key: string, update: JobProgress | null | ((job: JobProgress | null) => JobProgress | null)) => void
  /** Values applied by the person, by report id: "Apply" on the panel, a table row click, a bookmark, a filter
   *  button (not the auto-fill of required parameters) */
  applyClicks: Record<number, number>
  /** applyClicks of the report when the button started its last run, by report id and widget id: the button
   *  waits for the next apply, so the same values are not sent twice by mistake */
  runApplyClicks: Record<string, number>
  /** Parameter panels with parameters shown now, by report id: without one there is nothing to apply, and a
   *  button does not wait */
  panels: Record<number, number>
  /** The panel holds values changed by the person and not applied, by report id: a button sends the applied
   *  values, so it waits until they are applied */
  dirty: Record<number, boolean>
  markApplied: (reportId: number) => void
  markRunStarted: (key: string, reportId: number) => void
  addPanel: (reportId: number, delta: 1 | -1) => void
  setDirty: (reportId: number, dirty: boolean) => void
}

export const jobRunKey = (reportId: number, widgetId?: number) => `${reportId}:${widgetId ?? ''}`

export const useJobRunStore = create<JobRunState>((set, get) => ({
  jobs: {},
  setJob: (key, update) => {
    const current = get().jobs[key] ?? null
    const next = typeof update === 'function' ? update(current) : update
    if (next === current) return
    set(state => ({ jobs: { ...state.jobs, [key]: next } }))
  },
  applyClicks: {},
  runApplyClicks: {},
  markApplied: reportId =>
    set(state => ({ applyClicks: { ...state.applyClicks, [reportId]: (state.applyClicks[reportId] ?? 0) + 1 } })),
  markRunStarted: (key, reportId) =>
    set(state => ({ runApplyClicks: { ...state.runApplyClicks, [key]: state.applyClicks[reportId] ?? 0 } })),
  panels: {},
  addPanel: (reportId, delta) =>
    set(state => ({ panels: { ...state.panels, [reportId]: Math.max(0, (state.panels[reportId] ?? 0) + delta) } })),
  dirty: {},
  setDirty: (reportId, dirty) => {
    if ((get().dirty[reportId] ?? false) === dirty) return
    set(state => ({ dirty: { ...state.dirty, [reportId]: dirty } }))
  },
}))

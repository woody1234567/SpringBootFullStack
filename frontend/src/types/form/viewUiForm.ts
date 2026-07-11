// view-ui-plus does not export TypeScript types for its Form component instance
// or validation rules, so a minimal shape is declared locally for what this
// project actually uses.
export interface ViewUiFormInstance {
  validate: (callback: (valid: boolean) => void) => void
}

export interface ViewUiFormRuleItem {
  required?: boolean
  type?: string
  min?: number
  max?: number
  message?: string
  trigger?: string
  validator?: (rule: unknown, value: string, callback: (error?: Error) => void) => void
}

export type ViewUiFormRules = Record<string, ViewUiFormRuleItem[]>

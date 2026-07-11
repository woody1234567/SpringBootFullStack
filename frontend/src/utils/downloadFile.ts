const UTF8_BOM = '﻿'

export function downloadTextFile(filename: string, content: string, mimeType = 'text/plain;charset=utf-8;'): void {
  const blob = new Blob([UTF8_BOM + content], { type: mimeType })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  URL.revokeObjectURL(url)
}

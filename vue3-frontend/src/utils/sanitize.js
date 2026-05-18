import DOMPurify from 'dompurify'

const DEFAULT_CONFIG = {
  ALLOWED_TAGS: ['b', 'strong', 'i', 'em', 'u', 'br', 'p', 'span'],
  ALLOWED_ATTR: []
}

export function sanitizeHtml(value, config = DEFAULT_CONFIG) {
  return DOMPurify.sanitize(String(value ?? ''), config)
}

export function sanitizeText(value) {
  return DOMPurify.sanitize(String(value ?? ''), {
    ALLOWED_TAGS: [],
    ALLOWED_ATTR: []
  })
}

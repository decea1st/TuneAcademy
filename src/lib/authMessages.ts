/** The backend's explanation (ApiError.message) when there is one, otherwise a generic message. */
export function formatAuthError(err: unknown): string {
  if (err instanceof Error && err.message) return err.message;
  return "Something went wrong. Please try again.";
}

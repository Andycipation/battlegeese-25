"""
A script for in case we wanna try to analyze some Old But Gold stats, like how many units of each type the spawn.

It's not obvious how we'd get that information from the replay file though.
"""

from urllib.parse import urlparse, parse_qs

def extract_game_source(url: str) -> str:
    """Extracts the gameSource URL from the provided URL."""
    parsed_url = urlparse(url)
    query_params = parse_qs(parsed_url.query)
    
    # Extract the gameSource parameter and return it
    game_source = query_params.get('gameSource')
    if game_source:
        return game_source[0]
    else:
        raise ValueError("gameSource parameter not found in the URL")

# Example usage
url = "https://releases.battlecode.org/client/battlecode25/3.0.0/index.html?gameSource=https%3A%2F%2Fstorage.googleapis.com%2Fmitbattlecode-production-secure%2Fepisode%2Fbc25java%2Freplays%2F78e8e1aa-6423-4db4-b6f5-03da49fb3745.bc25java&page=Game"
try:
    game_source_url = extract_game_source(url)
    print("Extracted gameSource URL:", game_source_url)
except ValueError as e:
    print(e)

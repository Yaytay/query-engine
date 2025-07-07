#!/usr/bin/env python3

import base64
import hashlib
import json
import os
import requests
import time
from collections import defaultdict
from pathlib import Path
from urllib.parse import urlparse

class HttpRequest:
  def __init__(self, method: str, proto: str, uri: str, auth: str, accept: str):
    self.method = method
    if proto:
      self.uri = f"{proto}:/{uri}"
    else:
      self.uri = uri
    self.auth = auth
    self.accept = accept
    self.path = urlparse(self.uri).path


  def __eq__(self, other):
    if not isinstance(other, HttpRequest):
      return False
    return (self.method, self.uri, self.auth, self.accept) == (other.method, other.uri, other.auth, self.accept)

  def __hash__(self):
    return hash((self.method, self.uri, self.auth, self.accept))

  def __lt__(self, other):
    if not isinstance(other, HttpRequest):
      return NotImplemented
    return (self.method, self.uri, self.auth, self.accept) < (other.method, other.uri, other.auth, self.accept)

  def __repr__(self):
    return self.tostring(False)

  def tostring(self, redacted):
    acc = ''
    if self.accept:
      acc = f" -H 'Accept: {self.accept}' "
    meth = ''
    if self.method != 'GET':
      meth = f" -X {self.method}"
    authstr= ''
    if self.auth:
      if self.auth.startswith('Basic '):
        encoded_credentials = self.auth.split(" ", 1)[1]
        decoded_bytes = base64.b64decode(encoded_credentials)
        decoded_credentials = decoded_bytes.decode("utf-8")
        username, password = decoded_credentials.split(":", 1)
        if redacted:
          authstr = f" -u '{username}:**********'"
        else:
          authstr = f" -u '{decoded_credentials}'"
      else:
        if redacted:
          authstr = f" -H 'Authorization: ************'"
        else:
          authstr = f" -H 'Authorization: {self.auth}'"

    return f"curl -v{meth}{acc}{authstr} '{self.uri}'"

  def username(self):
    if self.auth:
      if self.auth.startswith('Basic '):
        encoded_credentials = self.auth.split(" ", 1)[1]
        decoded_bytes = base64.b64decode(encoded_credentials)
        decoded_credentials = decoded_bytes.decode("utf-8")
        username, password = decoded_credentials.split(":", 1)
        return username
    return self.auth


  def change_host(self, new_base: str):
    new_uri = self.uri.replace('https://query.gtios.com/', new_base)
    return HttpRequest(self.method, None, new_uri, self.auth, self.accept)

  def send(self):
    headers = {}
    if self.auth:
      headers["Authorization"] = self.auth
    if self.accept:
      headers["Accept"] = self.accept
    response = requests.request(self.method, self.uri, headers=headers)
    self.status_code = response.status_code
    return response.status_code, response.headers, response.content
    # return response.status_code, response.headers, response.content.decode('utf-8', errors='replace')

  def stable_hash(self):
    data = f"{self.method},{self.uri},{self.auth},{self.accept}".encode()
    return hashlib.sha256(data).hexdigest()[:16]


def get_field(obj, field, default):
  if field in obj:
    return obj[field].strip()
  else:
    return default

def get_header(obj, header, default):
  if 'http_http_request_line' not in obj:
    return default
  headers = obj['http_http_request_line']
  start = header + ': '
  for line in headers:
    if line.startswith(start):
      return line[len(start):-2]
  return default

def process_json_object(obj):
  """Function to process each parsed JSON object."""
  if 'http_http_request_method' in obj:
    method = get_field(obj, 'http_http_request_method', 'get')
    proto = get_header(obj, 'X-Forwarded-Proto', 'hTTp')
    uri = get_field(obj, 'http_http_request_uri', '')
    auth = get_header(obj, 'Authorization', None)
    accept = get_header(obj, 'Accept', None)
    return HttpRequest(method, proto, uri, auth, accept)

def make_request(outdir, base_name, req):
  filename = f"{outdir}/{base_name}.req"
  with open(filename, "w") as file:
    file.write(f"{repr(req)}\n")
  start_time = time.time()
  (status, headers, response) = req.send()
  end_time = time.time()
  print(f" {status} ({(end_time - start_time):.3f}s)", end=' ', flush=True)
  filename = f"{outdir}/{base_name}.status"
  with open(filename, "w") as file:
    file.write(f"{status}\n")
  filename = f"{outdir}/{base_name}.info"
  with open(filename, "w") as file:
    file.write(f"{status}\n{json.dumps(dict(headers), indent=2)}\n{(end_time - start_time):.3f}\n")

  filename = f"{outdir}/{base_name}.out"
  contentType = headers['Content-Type'] if 'Content-Type' in headers else ''
  if contentType == 'application/json':
    filename = filename + ".json"
  elif contentType.startswith('text/csv'):
    filename = filename + ".csv"
  elif contentType.startswith('application/rss+xml'):
    filename = filename + ".rss"
  elif contentType.startswith('application/xml'):
    filename = filename + ".xml"
  else:
    print(f'ContentType: "{contentType}"', end=' ', flush=True)

  with open(filename, "wb") as file:
    # To save time I only compare the first 1MB of the response
    file.write(response[:1024*1024])
  return filename

def compare_files(file1, file2, chunk_size=8192, context_size=10):
  try:
    with open(file1, 'rb') as f1, open(file2, 'rb') as f2:
      position = 0
      buffer1 = b''
      buffer2 = b''

      while True:
        chunk1 = f1.read(chunk_size)
        chunk2 = f2.read(chunk_size)

        # Add chunks to buffers
        buffer1 += chunk1
        buffer2 += chunk2

        # Find the minimum length to compare
        min_len = min(len(buffer1), len(buffer2))

        # Compare byte by byte in the current buffers
        for i in range(min_len):
          if buffer1[i] != buffer2[i]:
            # Found first difference at position + i
            diff_position = position + i

            # Calculate context boundaries
            start_pos = max(0, diff_position - context_size)
            end_pos = diff_position + context_size + 1

            # We need to read more data if we don't have enough context
            while len(buffer1) < end_pos - position or len(buffer2) < end_pos - position:
              additional1 = f1.read(chunk_size)
              additional2 = f2.read(chunk_size)

              if not additional1 and not additional2:
                break

              buffer1 += additional1
              buffer2 += additional2

            # Extract context from both files
            context1 = buffer1[start_pos - position:end_pos - position]
            context2 = buffer2[start_pos - position:end_pos - position]

            return {
              'identical': False,
              'difference_position': diff_position,
              'context': {
                'file1': context1,
                'file2': context2,
                'start_position': start_pos,
                'end_position': end_pos
              }
            }

        # Update position and remove processed bytes from buffers
        position += min_len
        buffer1 = buffer1[min_len:]
        buffer2 = buffer2[min_len:]

        # Check if we've reached the end of both files
        if not chunk1 and not chunk2:
          if len(buffer1) == 0 and len(buffer2) == 0:
            return None  # Files are identical
          # If buffers have different lengths, files are different
          elif len(buffer1) != len(buffer2):
            # Handle case where files have different lengths
            diff_position = position
            start_pos = max(0, diff_position - context_size)
            end_pos = diff_position + context_size + 1

            context1 = buffer1[start_pos - position:end_pos - position] if buffer1 else b''
            context2 = buffer2[start_pos - position:end_pos - position] if buffer2 else b''

            return {
              'identical': False,
              'difference_position': diff_position,
              'context': {
                'file1': context1,
                'file2': context2,
                'start_position': start_pos,
                'end_position': end_pos
              }
            }

        # If one file ended before the other
        elif not chunk1 or not chunk2:
          # Find the position where one file ends
          diff_position = position
          start_pos = max(0, diff_position - context_size)
          end_pos = diff_position + context_size + 1

          context1 = buffer1[start_pos - position:end_pos - position] if buffer1 else b''
          context2 = buffer2[start_pos - position:end_pos - position] if buffer2 else b''

          return {
            'identical': False,
            'difference_position': diff_position,
            'context': {
              'file1': context1,
              'file2': context2,
              'start_position': start_pos,
              'end_position': end_pos
            }
          }

  except FileNotFoundError as e:
    print(f"Error: {e.filename} not found.")
    return {
      'identical': False,
      'error': f"FileNotFoundError: {e.filename} not found."
    }
  except IOError as e:
    print(f"Error: Unable to read file due to {e}")
    return {
      'identical': False,
      'error': f"IOError: {e}"
    }

  except FileNotFoundError as e:
    print(f"Error: {e.filename} not found.")
    return False
  except IOError as e:
    print(f"Error: Unable to read {e.filename} due to {e}")
    return False


def delete_output(base_name):
  for file in Path('qe1').glob(base_name + '*'):
    file.unlink()
  for file in Path('qe2').glob(base_name + '*'):
    file.unlink()

def is_empty_json(path):
  size = os.path.getsize(path)
  if size == 2:
    return open(path, 'rb').read() == b'{}'
  if size == 11:
    return open(path, 'rb').read() == b'{"data":[]}'
  return False


def group_requests_by_path(original_list):
  """
  Group HttpRequest objects by path, maintaining reverse count ordering.
  Args:
      original_list: List of tuples (HttpRequest, count)
  Returns:
      List of tuples (HttpRequest, count, List[(HttpRequest, count)])
      where the first HttpRequest is the one with the highest count for that path
  """
  # Group by path while preserving order
  path_groups = defaultdict(list)

  # Group all requests by their path
  for request, count in original_list:
    path_groups[request.path].append((request, count))

  # Create the result list
  result = []

  # For each path group, find the request with the highest count
  for path, requests in path_groups.items():
    # requests are already sorted by count (reverse) from original list
    # so the first one has the highest count
    top_request, top_count = requests[0]

    # Create the sub-list with all other requests for this path
    sub_list = requests[1:]  # All except the first (highest count)
    result.append((top_request, top_count, sub_list))

  # Sort result by the highest count for each path (reverse order)
  result.sort(key=lambda x: x[1], reverse=True)

  return result


def stream_json_objects(file_path):

  requests = defaultdict(int)
  count = 0;

  with open(file_path, "r") as file:
    buffer = ""
    for line in file:
      if buffer == "" and not line.startswith("{"):
        continue
      buffer += line.strip()
      if line == "}\n":  # Detect end of a JSON object
        try:
          obj = json.loads(buffer)  # Parse JSON object
          req = process_json_object(obj)
          if req:
            requests[req] += 1
            count += 1
        except json.JSONDecodeError:
          # print("failed:", buffer)
          pass  # Incomplete object, continue accumulating
        buffer = ""  # Reset buffer after processing

  # print(f"Read {count} requests")

  os.makedirs('qe1', exist_ok=True)
  os.makedirs('qe2', exist_ok=True)

  sorted_requests = sorted(requests.items(), key=lambda item: item[1], reverse=True)
  sorted_requests = group_requests_by_path(sorted_requests)

  for req, count, others in sorted_requests:
    if '/odata4/' in req.uri:
      print(f"Skipping odata request {req.username()} {count} {req.tostring(True)}")
      continue
    if '/api-docs/' in req.uri:
      print(f"Skipping docs request {req.username()} {count} {req.tostring(True)}")
      continue
    if 'https://query.gtios.com/' == req.uri:
      print(f"Skipping root request {req.username()} {count} {req.tostring(True)}")
      continue
    #      if count < 30:
    #        print(f"Skipping infrequent request {count} {req.tostring(True)}")
    #        continue
    #      if count > 50:
    #        print(f"Skipping frequent request alreqady sorted {count} {req.tostring(True)}")
    #        continue


    base_name = req.stable_hash()
    print(f"{count}\t{base_name}\t{req.username()}\t{req.uri}", end='', flush=True)
    process_req(req, base_name, count)
    for sub_req, sub_count in others:
      sub_base_name = sub_req.stable_hash()
      print(f"    {sub_count}\t{sub_base_name}\t{sub_req.username()}\t{sub_req.uri}", end='', flush=True)
      process_req(sub_req, sub_base_name, sub_count)

def process_req(req, base_name, count):
  out1 = make_request('qe1', base_name, req)
  req2 = req.change_host('https://query-engine-qe1.test-swarm-01.groupgti.net/')
  out2 = make_request('qe2', base_name, req2)

  if '/Link/' in req.uri and is_empty_json(out1) and is_empty_json(out2):
    matched = True
  else:
    matched = compare_files(out1, out2)
  if matched == None:
    print(f"{out1} == {out2}")
    delete_output(base_name)
  elif 'identical' in matched and matched['identical']:
    print(f"{out1} == {out2}")
    delete_output(base_name)
  else:
    if req.status_code > 299 and req2.status_code > 299:
      delete_output(base_name)
      print(f"{out1} XX {out2}")
    elif base_name in ('17dfec42ee028144', '725b6a7900e6c090'):
      # Leeds have created two dynamic fields that differ only in case, QE2 gets it right, QE1 doesn't
      # /data/TARGETconnect/placement_records
      #delete_output(base_name)
      delete_output(base_name)
      print(f"{out1} <> {out2}")
    elif '/direct/Syndicate/Vacancies' in req.uri:
      # RSS feeds have encoding issues on QE1
      # /direct/Syndicate/Vacancies
      delete_output(base_name)
      print(f"{out1} <> {out2}")
    elif req.username() == 'brian.davis@leeds-services.targetconnect.net':
      # Brian Davies should not be making requests, QE1 and QE2 errors differ
      delete_output(base_name)
      print(f"{out1} <> {out2}")
    elif base_name in ('e3bccf7bc389fbd3'):
      # Leeds Arts have some dodgy data that means that QE1 and QE2 produce different results
      delete_output(base_name)
      print(f"{out1} <> {out2}")
    elif base_name in ('efe338ec68bea36f'):
      # InfernoThirdPartyAuth/opportunities produces incorrect results on QE1
      delete_output(base_name)
      print(f"{out1} <> {out2}")

    else:
      print(f"{out1} != {out2}\t@{matched['difference_position']}\t{matched['context']['file1']} != {matched['context']['file2']}")



# Example usage:
if __name__ == "__main__":
  stream_json_objects("/mnt/qe/njt/query-engine-requests.json")

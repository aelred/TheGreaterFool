import csv
from optparse import OptionParser
from pprint import pprint

parser = OptionParser()

options, args = parser.parse_args()
fname = args[0]

def _split(l, n):
    for i in xrange(0, len(l), n):
        yield l[i:i+n]

def load_info(fname):
    info = {
        'agents': {},
        'auctions': {}
    }

    resources = {
        '0': 'flight a',
        '1': 'flight d',
        '2': 'hotel TT',
        '3': 'hotel SS',
        '4': 'ent 1',
        '5': 'ent 2',
        '6': 'ent 3'
    }

    auction_to_name = {}
    agent_to_id = {}
    agent_to_name = {}

    def start_game(time, game_id, start_time, end_time, u_game_id=None, 
                   game_type=None, num_agents=None):
        # start of game
        info['start'] = int(start_time)
        info['end'] = int(end_time)

    def version(time, server_version, server_name):
        pass

    def add_agent(time, agent_name, agent_id):
        info['agents'][agent_name] = {
            'clients': []
        }
        agent_to_id[agent_name] = agent_id
        agent_to_name[agent_id] = agent_name

    def client_prefs(time, game_id, agent_id, *prefs):
        agent = agent_to_name[agent_id]
        for c in _split(prefs, 6):
            info['agents'][agent]['clients'].append({
                'in': c[0],
                'out': c[1],
                'hpref': c[2],
                'e1': c[3],
                'e2': c[4],
                'e3': c[5]
            })

    def start_auction(time, *auctions):
        for a in _split(auctions, 3):
            auction_id, resource, day = a
            resource = resources[resource]
            name = "%s %s" % (resource, day)
            auction_to_name[auction_id] = name
            info['auctions'][name] = {
                'resource': resource,
                'day': day,
                'sell_price': [],
                'buy_price': []
            }

    def transaction(time, buyer_id, seller_id, auction_id,
                    quantity, price, transaction_id=None):
        pass

    def quote(time, auction_id, sell_price, buy_price, *hqw):
        auction = info['auctions'][auction_to_name[auction_id]]
        auction['sell_price'].append({'time': time, 'price': float(sell_price)})
        auction['buy_price'].append({'time': time, 'price': float(buy_price)})

    def bid(time, bid_id, agent_id, auction_id, bid_type, 
            process_status, *bids):
        pass

    def close_auction(time, auction_id):
        pass
    
    def end_game(time, game_id, u_game_id=None):
        pass

    def final_alloc(time, game_id, agent_id, *allocs):
        pass

    def final_score(time, game_id, agent_id, score, penalty, utility, 
                    calc_time=None):
        pass
    
    handlers = {
        'g': start_game,
        'v': version,
        'a': add_agent,
        'c': client_prefs,
        'u': start_auction,
        't': transaction,
        'q': quote,
        'b': bid,
        'z': close_auction,
        'x': end_game,
        'l': final_alloc,
        's': final_score
    }

    with open(fname) as f:
        reader = csv.reader(f)
        for r in reader:
            t = r[1]
            time = int(r[0]) - info.get('start', 0)
            args = r[2:]
            f = handlers[t]
            f(time, *args)

    return info

pprint(load_info(fname))

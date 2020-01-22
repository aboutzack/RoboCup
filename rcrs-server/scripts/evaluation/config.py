all_teams = ["AIT", "RIO", "MRL", "CSU","BAS","MIX"]
final_teams = ["AIT", "MRL", "CSU"]
technical_teams = ["tAIT", "tRIO", "tMRL", "tCSU"]

team_names = {
    # "BAS" : "Baseline (no agents)",
    "AIT" : "AIT-rescue",
    "RIO" : "Ri-One",
    "MRL" : "MRL",
    "CSU" : "CSU-YUNLU",
    "BAS" : "baseline",
    "MIX": "Mix",

    "tAIT" : "tAIT-rescue",
    "tRIO" : "tRi-One",
    "tMRL" : "tMRL",
    "tCSU" : "tCSU-YUNLU",
}


title = "RoboCup 2019 Rescue Simulation League"

autoRefresh = True

daytest = {'name' : "Day test",
        'shortname' : "daytest",
        'maps' : ["KobeTest"],
        'teams' : all_teams}


day1 = {'name' : "Day 1",
        'shortname' : "Day1",
        'maps' : ["Kobe1", "Paris1", "VC1","Sydney1"],
        'teams' : all_teams}

day2 = {'name' : "Day 2",
        'shortname' : "Day2",
        'maps' : ["SydneyS1","Joao1","Istanbul1","NY1"],
	'merge_with' : day1,
        'teams' : all_teams}

day_technical = {'name' : "Technical",
        'shortname' : "technical",
        'maps' : ["tSFrio","tBerlinCSU","tKobeAIT"],
        'teams' : technical_teams}


semi = {'name' : "Semi final",
        'shortname' : "Semi",
        'maps' : ["Eindhoven1", "Montreal1", "Sakae1","Mexico1","Berlin1","SF1","Presentation"],
	'highlight' : 4,
        'teams' : all_teams}


final = {'name' : "Finals",
         'shortname' : "final",
         'maps' : ["SydneyS2","SF2","VC2","Paris2","Eindhoven2","Berlin2","Presentation2"],
         'teams' : final_teams, 
         'show_ranks' : 1}

#rounds = [day1, day2, semi, final, technical]
rounds = [day1,day2,semi,day_technical, final]

# semi_teams = ["RAK", "SBC", "POS", "IAM", "MRL", "RI1", "SEU", "RMA"]
# final_teams = ["POS", "IAM", "SEU", "RMA"]

# day1 = {'name' : "Preliminaries Day 1",
#         'shortname' : "Preliminary1",
#         'maps' : ["VC1", "Paris1", "Kobe1", "Berlin1", "Istanbul1"],
#         'teams' : all_teams}

# day2 = {'name' : "Preliminaries Day 2",
#         'shortname' : "Preliminary2",
#         'maps' : ["Kobe2", "Paris2", "Istanbul2", "Berlin2", "VC2"],
#         'teams' : all_teams
#         'merge_with' : day1
#         'highlight' : 8}

# semi = {'name' : "Semifinals",
#         'shortname' : "Semifinals",
#         'maps' : ["Kobe2", "Paris2", "Istanbul2", "Berlin2", "VC2"],
#         'teams' : semi_teams,
#         'highlight' : 4}

# final = {'name' : "Finals",
#         'shortname' : "Finals",
#         'maps' : ["Kobe2", "Paris2", "Istanbul2", "Berlin2", "VC2"],
#         'teams' : ["Paris5", "Berlin5", "Kobe4", "Istanbul5", "VC5"],
#         'show_ranks' : 3}

# rounds = [day1, day2, semi, final]

log_location = "logs/RCAP2017"
add_downloads = True
